#!/usr/bin/env python3
"""Prepares the official DeepFilterNet 3 ONNX graphs for streaming inference.

The graphs shipped in `DeepFilterNet3_onnx.tar.gz` are exported for batch inference with
the sequence length `S` as a free dimension. Two properties prevent direct frame-by-frame
use with ONNX Runtime:

  1. The recurrent layers (`GRU`) are initialized with a zero hidden state on every
     `Run()`; the exported graphs do not expose it.
  2. The causal convolution front-ends rely on the graph's own zero padding, which would
     replace the real history of every frame with zeros when feeding a single frame.

This tool applies a minimal, deterministic transformation:

  * every `GRU` node gets its `initial_h` input replaced by a new graph input
    `state_in_<i>` and its `Y_h` output exposed as `state_out_<i>`;
  * the time-axis `Pad` nodes of the encoder's input convolutions are disabled, so the
    caller supplies real history frames (3-frame window for `enc`, 5-frame for `df_dec`);
  * a `Slice` keeps only the last time step after the causal convolution stack (and for
    the `emb`/`e*` inputs of the decoders), so each model produces exactly one output
    frame per call.

The transformed graphs are verified against the originals in ONNX Runtime (see
`verify_streaming_equivalence`) and are numerically identical for every frame once the
documented history window is supplied.

Usage:
    python tools/prepare_models.py --model-dir <dir with enc.onnx/erb_dec.onnx/df_dec.onnx> \
                                   --out-dir <dir>
"""

import argparse
import os

import numpy as np
import onnx
import onnxruntime as ort
from onnx import TensorProto, helper, numpy_helper

INT_MAX = 9223372036854775807
STATE_DIM = 256


def _resolve(graph, name):
    for init in graph.initializer:
        if init.name == name:
            return numpy_helper.to_array(init)
    for node in graph.node:
        if node.op_type == "Constant" and node.output[0] == name:
            for attribute in node.attribute:
                if attribute.name == "value":
                    return numpy_helper.to_array(attribute.t)
    return None


def _disable_time_padding(graph, pad_node, rank=4):
    """Replaces the (dynamic) pads input of a Pad node with zeros."""
    name = pad_node.name.replace("/", "_") + "_zero_pads"
    graph.initializer.append(numpy_helper.from_array(np.zeros(2 * rank, dtype=np.int64), name))
    pad_node.input[1] = name


def _keep_last_time_step(graph, tensor, axis):
    """Restricts `tensor` to its last index along `axis` for every consumer."""
    starts = numpy_helper.from_array(np.array([-1], dtype=np.int64), tensor + "_s_starts")
    ends = numpy_helper.from_array(np.array([INT_MAX], dtype=np.int64), tensor + "_s_ends")
    axes = numpy_helper.from_array(np.array([axis], dtype=np.int64), tensor + "_s_axes")
    graph.initializer.extend([starts, ends, axes])

    produced = any(out == tensor for node in graph.node for out in node.output)
    if produced:
        # Keep the tensor name stable: rename the producer and slice back into the name.
        pre = tensor + "_pre"
        for node in graph.node:
            for i, value in enumerate(node.output):
                if value == tensor:
                    node.output[i] = pre
        graph.node.append(helper.make_node(
            "Slice", [pre, starts.name, ends.name, axes.name], [tensor],
            name=tensor + "_slice"))
        return

    # Graph input: slice into a new tensor and rewire the consumers.
    sliced = tensor + "_lastframe"
    slice_node = helper.make_node(
        "Slice", [tensor, starts.name, ends.name, axes.name], [sliced],
        name=tensor + "_slice")
    graph.node.append(slice_node)
    for node in graph.node:
        if node.name == slice_node.name:
            continue
        for i, value in enumerate(node.input):
            if value == tensor:
                node.input[i] = sliced


def _expose_gru_states(graph):
    count = 0
    for node in graph.node:
        if node.op_type != "GRU":
            continue
        weights = _resolve(graph, node.input[1])
        directions, _, hidden = weights.shape
        if directions != 1:
            raise SystemExit("bidirectional GRUs are not supported by the streaming engine")
        in_name, out_name = f"state_in_{count}", f"state_out_{count}"
        node.input[5] = in_name
        node.output[1] = out_name
        graph.input.append(helper.make_tensor_value_info(in_name, TensorProto.FLOAT,
                                                         [1, 1, hidden]))
        graph.output.append(helper.make_tensor_value_info(out_name, TensorProto.FLOAT,
                                                          [1, None, hidden]))
        count += 1
    return count


def prepare_encoder(src, dst):
    model = onnx.load(src)
    graph = model.graph
    for prefix in ("/erb_conv0/0/Pad", "/df_conv0/0/Pad"):
        for node in graph.node:
            if node.op_type == "Pad" and node.name.startswith(prefix):
                _disable_time_padding(graph, node)
    _keep_last_time_step(graph, "e0", axis=2)
    _keep_last_time_step(graph, "c0", axis=2)
    states = _expose_gru_states(graph)
    onnx.save(model, dst)
    return states


def prepare_erb_decoder(src, dst):
    model = onnx.load(src)
    graph = model.graph
    _keep_last_time_step(graph, "emb", axis=1)
    for name in ("e0", "e1", "e2", "e3"):
        _keep_last_time_step(graph, name, axis=2)
    states = _expose_gru_states(graph)
    onnx.save(model, dst)
    return states


def prepare_df_decoder(src, dst):
    model = onnx.load(src)
    graph = model.graph
    _keep_last_time_step(graph, "emb", axis=1)
    conv_output = None
    for node in graph.node:
        if node.name.startswith("/df_convp/") and node.op_type in ("Conv", "Add"):
            conv_output = node.output[0]
    if conv_output is None:
        raise SystemExit("could not locate the df_convp output")
    _keep_last_time_step(graph, conv_output, axis=2)
    states = _expose_gru_states(graph)
    onnx.save(model, dst)
    return states


def verify_streaming_equivalence(src, stream_path, name, history, state_dim=STATE_DIM):
    """Compares the batch graph with the per-frame graph over random inputs."""
    rng = np.random.RandomState(11)
    frames = 24
    batch = ort.InferenceSession(src, providers=["CPUExecutionProvider"])
    stream = ort.InferenceSession(stream_path, providers=["CPUExecutionProvider"])
    stream_inputs = [i.name for i in stream.get_inputs()]
    stream_outputs = [o.name for o in stream.get_outputs()]

    if name == "enc":
        inputs = {"feat_erb": (rng.randn(1, 1, frames, 32) * 0.5).astype(np.float32),
                  "feat_spec": (rng.randn(1, 2, frames, 96) * 0.5).astype(np.float32)}
        primary, primary_batch, batch_time_axis = "emb", "emb", 1
    elif name == "erb_dec":
        inputs = {"emb": (rng.randn(1, frames, 512) * 0.3).astype(np.float32),
                  "e3": (rng.randn(1, 64, frames, 8) * 0.3).astype(np.float32),
                  "e2": (rng.randn(1, 64, frames, 8) * 0.3).astype(np.float32),
                  "e1": (rng.randn(1, 64, frames, 16) * 0.3).astype(np.float32),
                  "e0": (rng.randn(1, 64, frames, 32) * 0.3).astype(np.float32)}
        primary, primary_batch, batch_time_axis = "m", "m", 2
    else:
        inputs = {"emb": (rng.randn(1, frames, 512) * 0.3).astype(np.float32),
                  "c0": (rng.randn(1, 64, frames, 96) * 0.3).astype(np.float32)}
        primary, primary_batch, batch_time_axis = "coefs", "coefs", 1

    reference = dict(zip([o.name for o in batch.get_outputs()], batch.run(None, inputs)))
    states = [np.zeros((1, 1, state_dim), np.float32)
              for _ in stream_inputs if _.startswith("state_in_")]
    worst = 0.0
    window = history + 1
    for t in range(frames):
        feed = {}
        for key, value in inputs.items():
            time_axis = 2 if value.ndim == 4 else 1
            shape = list(value.shape)
            shape[time_axis] = window
            buffer = np.zeros(shape, np.float32)
            count = min(window, t + 1)
            src_slice = [slice(None)] * value.ndim
            src_slice[time_axis] = slice(t + 1 - count, t + 1)
            dst_slice = [slice(None)] * value.ndim
            dst_slice[time_axis] = slice(window - count, window)
            buffer[tuple(dst_slice)] = value[tuple(src_slice)]
            feed[key] = buffer
        for i, sname in enumerate([n for n in stream_inputs if n.startswith("state_in_")]):
            feed[sname] = states[i]
        out = dict(zip(stream_outputs, stream.run(None, feed)))
        states = [out[s.replace("state_in_", "state_out_")] for s in
                  [n for n in stream_inputs if n.startswith("state_in_")]]
        if t < history:
            continue
        got = np.asarray(out[primary]).reshape(-1)
        want = np.take(np.asarray(reference[primary_batch]), t, axis=batch_time_axis)
        worst = max(worst, float(np.abs(got - want.reshape(-1)).max()))
    return worst


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)

    jobs = (
        ("enc", prepare_encoder, 2),
        ("erb_dec", prepare_erb_decoder, 0),
        ("df_dec", prepare_df_decoder, 4),
    )
    for name, prepare, history in jobs:
        src = os.path.join(args.model_dir, name + ".onnx")
        dst = os.path.join(args.out_dir, name + "_stream.onnx")
        states = prepare(src, dst)
        error = verify_streaming_equivalence(src, dst, name, history)
        print(f"{name}: {states} recurrent state(s), history={history}, "
              f"max|stream-batch|={error:.3e}")
        if error > 1e-5:
            raise SystemExit(f"{name} streaming graph does not match the batch graph")


if __name__ == "__main__":
    main()