#include "onnx_backend.h"

#include <nnapi_provider_factory.h>
#include <onnxruntime_cxx_api.h>
#include <algorithm>
#include <cstring>
#include <numeric>
#include <sstream>

namespace dfn::enhancer {
namespace {

Ort::Env& shared_env() {
    static Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "dfn3");
    return env;
}

Ort::MemoryInfo& memory_info() {
    static Ort::MemoryInfo info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    return info;
}

bool is_dynamic(int64_t dim) {
    return dim < 0;
}

const TensorSpec* find(const std::vector<TensorSpec>& specs, const std::string& name) {
    for (const TensorSpec& spec : specs) {
        if (spec.name == name) {
            return &spec;
        }
    }
    return nullptr;
}

bool expect_shape(const std::vector<TensorSpec>& specs, const std::string& graph,
                  const std::string& name, const std::vector<int64_t>& expected,
                  std::string* error) {
    const TensorSpec* spec = find(specs, name);
    if (spec == nullptr) {
        *error = graph + " is missing tensor '" + name + "'";
        return false;
    }
    if (spec->element_type != ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT) {
        *error = graph + " tensor '" + name + "' is not float32";
        return false;
    }
    if (spec->shape.size() != expected.size()) {
        std::ostringstream message;
        message << graph << " tensor '" << name << "' has rank " << spec->shape.size()
                << ", expected " << expected.size();
        *error = message.str();
        return false;
    }
    for (std::size_t i = 0; i < expected.size(); ++i) {
        if (expected[i] > 0 && !is_dynamic(spec->shape[i]) && spec->shape[i] != expected[i]) {
            std::ostringstream message;
            message << graph << " tensor '" << name << "' dimension " << i << " is "
                    << spec->shape[i] << ", expected " << expected[i];
            *error = message.str();
            return false;
        }
    }
    return true;
}

std::vector<std::string> state_names(const std::vector<TensorSpec>& specs) {
    std::vector<std::string> names;
    for (const TensorSpec& spec : specs) {
        if (spec.name.rfind("state_in_", 0) == 0) {
            names.push_back(spec.name);
        }
    }
    std::sort(names.begin(), names.end());
    return names;
}

std::string state_output_name(const std::string& input) {
    static const std::string prefix = "state_in_";
    return "state_out_" + input.substr(prefix.size());
}

int hidden_size(const TensorSpec& spec) {
    int64_t total = 1;
    for (const int64_t dim : spec.shape) {
        total *= dim > 0 ? dim : 1;
    }
    return static_cast<int>(total);
}

std::vector<std::vector<float>> make_zero_state(const std::vector<TensorSpec>& inputs) {
    std::vector<std::vector<float>> state;
    for (const TensorSpec& spec : inputs) {
        if (spec.name.rfind("state_in_", 0) == 0) {
            state.emplace_back(static_cast<std::size_t>(hidden_size(spec)), 0.0f);
        }
    }
    return state;
}

// Copies a tensor held by ORT into `out`.
void copy_output(const Ort::Value& value, std::vector<float>& out) {
    const auto info = value.GetTensorTypeAndShapeInfo();
    const std::size_t count = info.GetElementCount();
    out.resize(count);
    std::memcpy(out.data(), value.GetTensorData<float>(), count * sizeof(float));
}

Ort::Value make_tensor(const std::vector<float>& data, const std::vector<int64_t>& shape) {
    return Ort::Value::CreateTensor<float>(
        memory_info(), const_cast<float*>(data.data()), data.size(), shape.data(), shape.size());
}

}  // namespace

struct OnnxBackend::Impl {
    ModelConfig config;

    std::unique_ptr<Ort::Session> encoder;
    std::unique_ptr<Ort::Session> erb_decoder;
    std::unique_ptr<Ort::Session> df_decoder;

    std::vector<TensorSpec> enc_inputs, enc_outputs;
    std::vector<TensorSpec> erb_inputs, erb_outputs;
    std::vector<TensorSpec> df_inputs, df_outputs;

    std::vector<std::string> enc_in_names, enc_out_names;
    std::vector<std::string> erb_in_names, erb_out_names;
    std::vector<std::string> df_in_names, df_out_names;
    std::vector<const char*> enc_in_ptrs, enc_out_ptrs;
    std::vector<const char*> erb_in_ptrs, erb_out_ptrs, df_in_ptrs, df_out_ptrs;

    std::vector<float> enc_erb, enc_spec;
    std::vector<float> erb_emb, erb_e0, erb_e1, erb_e2, erb_e3;
    std::vector<float> df_emb, df_c0;
    std::vector<float> out_mask, out_coefs;
};

namespace {

void assign_ptrs(const std::vector<std::string>& names, std::vector<const char*>& ptrs) {
    ptrs.clear();
    ptrs.reserve(names.size());
    for (const std::string& name : names) {
        ptrs.push_back(name.c_str());
    }
}

// Collects the names and shapes of a session's inputs.
void describe_session(Ort::Session& session, std::vector<std::string>& names,
                      std::vector<TensorSpec>& specs, bool inputs) {
    Ort::AllocatorWithDefaultOptions allocator;
    names.clear();
    specs.clear();
    const std::size_t count = inputs ? session.GetInputCount() : session.GetOutputCount();
    for (std::size_t i = 0; i < count; ++i) {
        auto name = inputs ? session.GetInputNameAllocated(i, allocator)
                           : session.GetOutputNameAllocated(i, allocator);
        names.emplace_back(name.get());
        auto type_info = inputs ? session.GetInputTypeInfo(i) : session.GetOutputTypeInfo(i);
        TensorSpec spec;
        spec.name = names.back();
        if (type_info.GetONNXType() == ONNX_TYPE_TENSOR) {
            auto tensor_info = type_info.GetTensorTypeAndShapeInfo();
            spec.shape = tensor_info.GetShape();
            spec.element_type = static_cast<int32_t>(tensor_info.GetElementType());
        }
        specs.push_back(std::move(spec));
    }
}

}  // namespace

OnnxBackend::OnnxBackend() : impl_(std::make_unique<Impl>()) {}

OnnxBackend::~OnnxBackend() = default;

const std::vector<TensorSpec>& OnnxBackend::encoder_inputs() const {
    return impl_->enc_inputs;
}
const std::vector<TensorSpec>& OnnxBackend::encoder_outputs() const {
    return impl_->enc_outputs;
}
const std::vector<TensorSpec>& OnnxBackend::erb_decoder_inputs() const {
    return impl_->erb_inputs;
}
const std::vector<TensorSpec>& OnnxBackend::df_decoder_inputs() const {
    return impl_->df_inputs;
}

std::unique_ptr<OnnxBackend> OnnxBackend::from_files(const std::string& encoder_path,
                                                     const std::string& erb_decoder_path,
                                                     const std::string& df_decoder_path,
                                                     const ModelConfig& config, Options options,
                                                     std::string* error) {
    auto backend = std::unique_ptr<OnnxBackend>(new OnnxBackend());
    try {
        Ort::SessionOptions session_options;
        session_options.SetIntraOpNumThreads(std::max(1, options.intra_op_threads));
        session_options.SetInterOpNumThreads(std::max(1, options.inter_op_threads));
        session_options.SetGraphOptimizationLevel(
            static_cast<GraphOptimizationLevel>(options.graph_optimization_level));
#ifdef __ANDROID__
        if (options.use_nnapi) {
            Ort::ThrowOnError(OrtSessionOptionsAppendExecutionProvider_Nnapi(session_options, 0));
        }
#else
        (void)options.use_nnapi;
#endif
        backend->impl_->encoder =
            std::make_unique<Ort::Session>(shared_env(), encoder_path.c_str(), session_options);
        backend->impl_->erb_decoder =
            std::make_unique<Ort::Session>(shared_env(), erb_decoder_path.c_str(), session_options);
        backend->impl_->df_decoder =
            std::make_unique<Ort::Session>(shared_env(), df_decoder_path.c_str(), session_options);
    } catch (const Ort::Exception& exception) {
        if (error != nullptr) {
            *error = std::string("failed to load ONNX model: ") + exception.what();
        }
        return nullptr;
    }
    if (!backend->_initialize(config, error)) {
        return nullptr;
    }
    return backend;
}

std::unique_ptr<OnnxBackend> OnnxBackend::from_memory(const std::vector<uint8_t>& encoder,
                                                      const std::vector<uint8_t>& erb_decoder,
                                                      const std::vector<uint8_t>& df_decoder,
                                                      const ModelConfig& config, Options options,
                                                      std::string* error) {
    auto backend = std::unique_ptr<OnnxBackend>(new OnnxBackend());
    try {
        Ort::SessionOptions session_options;
        session_options.SetIntraOpNumThreads(std::max(1, options.intra_op_threads));
        session_options.SetInterOpNumThreads(std::max(1, options.inter_op_threads));
        session_options.SetGraphOptimizationLevel(
            static_cast<GraphOptimizationLevel>(options.graph_optimization_level));
        backend->impl_->encoder = std::make_unique<Ort::Session>(
            shared_env(), encoder.data(), encoder.size(), session_options);
        backend->impl_->erb_decoder = std::make_unique<Ort::Session>(
            shared_env(), erb_decoder.data(), erb_decoder.size(), session_options);
        backend->impl_->df_decoder = std::make_unique<Ort::Session>(
            shared_env(), df_decoder.data(), df_decoder.size(), session_options);
    } catch (const Ort::Exception& exception) {
        if (error != nullptr) {
            *error = std::string("failed to load ONNX model from memory: ") + exception.what();
        }
        return nullptr;
    }
    if (!backend->_initialize(config, error)) {
        return nullptr;
    }
    return backend;
}

bool OnnxBackend::_initialize(const ModelConfig& config, std::string* error) {
    std::string message;
    impl_->config = config;
    Impl& impl = *impl_;

    describe_session(*impl.encoder, impl.enc_in_names, impl.enc_inputs, true);
    describe_session(*impl.encoder, impl.enc_out_names, impl.enc_outputs, false);
    describe_session(*impl.erb_decoder, impl.erb_in_names, impl.erb_inputs, true);
    describe_session(*impl.erb_decoder, impl.erb_out_names, impl.erb_outputs, false);
    describe_session(*impl.df_decoder, impl.df_in_names, impl.df_inputs, true);
    describe_session(*impl.df_decoder, impl.df_out_names, impl.df_outputs, false);

    const int64_t ch = config.conv_ch;
    const int64_t nb_erb = config.nb_erb;
    const int64_t nb_df = config.nb_df;
    const int64_t hidden = config.erb_hidden();
    const int64_t window = config.encoder_window_frames();
    const int64_t df_window = config.df_decoder_window_frames();

    auto fail = [&](const std::string& what) {
        if (error != nullptr) {
            *error = what;
        }
        return false;
    };

    // Encoder.
    if (!expect_shape(impl.enc_inputs, "enc", "feat_erb", {1, 1, window, nb_erb}, &message) ||
        !expect_shape(impl.enc_inputs, "enc", "feat_spec", {1, 2, window, nb_df}, &message) ||
        !expect_shape(impl.enc_outputs, "enc", "e0", {1, ch, 1, nb_erb}, &message) ||
        !expect_shape(impl.enc_outputs, "enc", "e1", {1, ch, 1, nb_erb / 2}, &message) ||
        !expect_shape(impl.enc_outputs, "enc", "e2", {1, ch, 1, nb_erb / 4}, &message) ||
        !expect_shape(impl.enc_outputs, "enc", "e3", {1, ch, 1, nb_erb / 4}, &message) ||
        !expect_shape(impl.enc_outputs, "enc", "emb", {1, 1, hidden}, &message) ||
        !expect_shape(impl.enc_outputs, "enc", "c0", {1, ch, 1, nb_df}, &message) ||
        !expect_shape(impl.enc_outputs, "enc", "lsnr", {1, 1, 1}, &message)) {
        return fail(message);
    }

    // ERB decoder.
    if (!expect_shape(impl.erb_inputs, "erb_dec", "emb", {1, 1, hidden}, &message) ||
        !expect_shape(impl.erb_inputs, "erb_dec", "e0", {1, ch, 1, nb_erb}, &message) ||
        !expect_shape(impl.erb_inputs, "erb_dec", "e1", {1, ch, 1, nb_erb / 2}, &message) ||
        !expect_shape(impl.erb_inputs, "erb_dec", "e2", {1, ch, 1, nb_erb / 4}, &message) ||
        !expect_shape(impl.erb_inputs, "erb_dec", "e3", {1, ch, 1, nb_erb / 4}, &message) ||
        !expect_shape(impl.erb_outputs, "erb_dec", "m", {1, 1, 1, nb_erb}, &message)) {
        return fail(message);
    }

    // DF decoder.
    if (!expect_shape(impl.df_inputs, "df_dec", "emb", {1, df_window, hidden}, &message) ||
        !expect_shape(impl.df_inputs, "df_dec", "c0", {1, ch, df_window, nb_df}, &message) ||
        !expect_shape(
            impl.df_outputs, "df_dec", "coefs", {1, 1, nb_df, 2 * config.df_order}, &message)) {
        return fail(message);
    }

    // Recurrent state inputs: [1, 1, H] each, paired with a state_out_<i> output.
    auto validate_state = [&](const std::vector<TensorSpec>& inputs,
                              const std::vector<TensorSpec>& outputs,
                              const std::string& graph,
                              const char* expected_output) -> bool {
        const std::vector<std::string> names = state_names(inputs);
        if (names.empty()) {
            return fail(graph + " has no recurrent state inputs (state_in_*)");
        }
        for (const std::string& name : names) {
            if (!expect_shape(inputs, graph, name, {1, 1, -1}, &message)) {
                return false;
            }
            if (find(outputs, state_output_name(name)) == nullptr) {
                return fail(graph + " is missing output '" + state_output_name(name) + "'");
            }
        }
        if (find(outputs, expected_output) == nullptr) {
            return fail(graph + " is missing output '" + expected_output + "'");
        }
        return true;
    };
    if (!validate_state(impl.enc_inputs, impl.enc_outputs, "enc", "lsnr") ||
        !validate_state(impl.erb_inputs, impl.erb_outputs, "erb_dec", "m") ||
        !validate_state(impl.df_inputs, impl.df_outputs, "df_dec", "coefs")) {
        return fail(message);
    }

    assign_ptrs(impl.enc_in_names, impl.enc_in_ptrs);
    assign_ptrs(impl.enc_out_names, impl.enc_out_ptrs);
    assign_ptrs(impl.erb_in_names, impl.erb_in_ptrs);
    assign_ptrs(impl.erb_out_names, impl.erb_out_ptrs);
    assign_ptrs(impl.df_in_names, impl.df_in_ptrs);
    assign_ptrs(impl.df_out_names, impl.df_out_ptrs);

    impl.enc_erb.assign(static_cast<std::size_t>(window * nb_erb), 0.0f);
    impl.enc_spec.assign(static_cast<std::size_t>(2 * window * nb_df), 0.0f);
    impl.erb_emb.assign(static_cast<std::size_t>(hidden), 0.0f);
    impl.erb_e0.assign(static_cast<std::size_t>(ch * nb_erb), 0.0f);
    impl.erb_e1.assign(static_cast<std::size_t>(ch * nb_erb / 2), 0.0f);
    impl.erb_e2.assign(static_cast<std::size_t>(ch * nb_erb / 4), 0.0f);
    impl.erb_e3.assign(static_cast<std::size_t>(ch * nb_erb / 4), 0.0f);
    impl.df_emb.assign(static_cast<std::size_t>(df_window * hidden), 0.0f);
    impl.df_c0.assign(static_cast<std::size_t>(ch * df_window * nb_df), 0.0f);

    if (error != nullptr) {
        error->clear();
    }
    return true;
}

void OnnxBackend::init_state(RecurrentState& state) const {
    state.encoder = make_zero_state(impl_->enc_inputs);
    state.erb_decoder = make_zero_state(impl_->erb_inputs);
    state.df_decoder = make_zero_state(impl_->df_inputs);
}

EncoderResult OnnxBackend::run_encoder(const float* feat_erb, int window_frames,
                                       const float* feat_spec, RecurrentState& state) {
    Impl& impl = *impl_;
    const ModelConfig& cfg = impl.config;
    const auto erb_count = static_cast<std::size_t>(window_frames * cfg.nb_erb);
    std::copy(feat_erb, feat_erb + erb_count, impl.enc_erb.begin());
    // Input layout: [window][2][nb_df]; tensor layout: [2][window][nb_df].
    for (int w = 0; w < window_frames; ++w) {
        for (int channel = 0; channel < 2; ++channel) {
            for (int f = 0; f < cfg.nb_df; ++f) {
                const std::size_t src =
                    (static_cast<std::size_t>(w) * 2 + static_cast<std::size_t>(channel)) *
                        static_cast<std::size_t>(cfg.nb_df) +
                    static_cast<std::size_t>(f);
                const std::size_t dst =
                    (static_cast<std::size_t>(channel) * static_cast<std::size_t>(window_frames) +
                     static_cast<std::size_t>(w)) *
                        static_cast<std::size_t>(cfg.nb_df) +
                    static_cast<std::size_t>(f);
                impl.enc_spec[dst] = feat_spec[src];
            }
        }
    }

    if (state.encoder.empty()) {
        state.encoder = make_zero_state(impl.enc_inputs);
    }

    std::vector<Ort::Value> inputs;
    inputs.reserve(impl.enc_in_names.size());
    for (const std::string& name : impl.enc_in_names) {
        if (name == "feat_erb") {
            inputs.push_back(make_tensor(impl.enc_erb, {1, 1, window_frames, cfg.nb_erb}));
        } else if (name == "feat_spec") {
            inputs.push_back(make_tensor(impl.enc_spec, {1, 2, window_frames, cfg.nb_df}));
        } else {
            int index = 0;
            for (const std::string& state_name : state_names(impl.enc_inputs)) {
                if (state_name == name) {
                    break;
                }
                ++index;
            }
            const std::vector<float>& value = state.encoder[static_cast<std::size_t>(index)];
            inputs.push_back(make_tensor(value, {1, 1, static_cast<int64_t>(value.size())}));
        }
    }

    auto outputs = impl.encoder->Run(Ort::RunOptions{nullptr},
                                     impl.enc_in_ptrs.data(),
                                     inputs.data(),
                                     inputs.size(),
                                     impl.enc_out_ptrs.data(),
                                     impl.enc_out_ptrs.size());

    EncoderResult result;
    const std::vector<std::string> state_inputs = state_names(impl.enc_inputs);
    for (std::size_t i = 0; i < impl.enc_out_names.size(); ++i) {
        const std::string& name = impl.enc_out_names[i];
        if (name == "e0") {
            copy_output(outputs[i], result.e0);
        } else if (name == "e1") {
            copy_output(outputs[i], result.e1);
        } else if (name == "e2") {
            copy_output(outputs[i], result.e2);
        } else if (name == "e3") {
            copy_output(outputs[i], result.e3);
        } else if (name == "emb") {
            copy_output(outputs[i], result.emb);
        } else if (name == "c0") {
            copy_output(outputs[i], result.c0);
        } else if (name == "lsnr") {
            const auto info = outputs[i].GetTensorTypeAndShapeInfo();
            result.lsnr = outputs[i].GetTensorData<float>()[info.GetElementCount() - 1];
        } else if (name.rfind("state_out_", 0) == 0) {
            for (std::size_t s = 0; s < state_inputs.size(); ++s) {
                if (name == state_output_name(state_inputs[s])) {
                    copy_output(outputs[i], state.encoder[s]);
                }
            }
        }
    }
    return result;
}

const std::vector<float>& OnnxBackend::run_erb_decoder(const EncoderResult& encoder,
                                                       RecurrentState& state) {
    Impl& impl = *impl_;
    const ModelConfig& cfg = impl.config;
    if (state.erb_decoder.empty()) {
        state.erb_decoder = make_zero_state(impl.erb_inputs);
    }
    std::copy(encoder.emb.begin(), encoder.emb.end(), impl.erb_emb.begin());
    std::copy(encoder.e0.begin(), encoder.e0.end(), impl.erb_e0.begin());
    std::copy(encoder.e1.begin(), encoder.e1.end(), impl.erb_e1.begin());
    std::copy(encoder.e2.begin(), encoder.e2.end(), impl.erb_e2.begin());
    std::copy(encoder.e3.begin(), encoder.e3.end(), impl.erb_e3.begin());

    const int64_t ch = cfg.conv_ch;
    std::vector<Ort::Value> inputs;
    inputs.reserve(impl.erb_in_names.size());
    for (const std::string& name : impl.erb_in_names) {
        if (name == "emb") {
            inputs.push_back(
                make_tensor(impl.erb_emb, {1, 1, static_cast<int64_t>(impl.erb_emb.size())}));
        } else if (name == "e0") {
            inputs.push_back(make_tensor(impl.erb_e0, {1, ch, 1, cfg.nb_erb}));
        } else if (name == "e1") {
            inputs.push_back(make_tensor(impl.erb_e1, {1, ch, 1, cfg.nb_erb / 2}));
        } else if (name == "e2") {
            inputs.push_back(make_tensor(impl.erb_e2, {1, ch, 1, cfg.nb_erb / 4}));
        } else if (name == "e3") {
            inputs.push_back(make_tensor(impl.erb_e3, {1, ch, 1, cfg.nb_erb / 4}));
        } else {
            int index = 0;
            for (const std::string& state_name : state_names(impl.erb_inputs)) {
                if (state_name == name) {
                    break;
                }
                ++index;
            }
            const std::vector<float>& value = state.erb_decoder[static_cast<std::size_t>(index)];
            inputs.push_back(make_tensor(value, {1, 1, static_cast<int64_t>(value.size())}));
        }
    }

    auto outputs = impl.erb_decoder->Run(Ort::RunOptions{nullptr},
                                         impl.erb_in_ptrs.data(),
                                         inputs.data(),
                                         inputs.size(),
                                         impl.erb_out_ptrs.data(),
                                         impl.erb_out_ptrs.size());

    const std::vector<std::string> state_inputs = state_names(impl.erb_inputs);
    for (std::size_t i = 0; i < impl.erb_out_names.size(); ++i) {
        const std::string& name = impl.erb_out_names[i];
        if (name == "m") {
            copy_output(outputs[i], impl.out_mask);
        } else if (name.rfind("state_out_", 0) == 0) {
            for (std::size_t s = 0; s < state_inputs.size(); ++s) {
                if (name == state_output_name(state_inputs[s])) {
                    copy_output(outputs[i], state.erb_decoder[s]);
                }
            }
        }
    }
    return impl.out_mask;
}

const std::vector<float>& OnnxBackend::run_df_decoder(const float* emb_window,
                                                      const float* c0_window,
                                                      RecurrentState& state) {
    Impl& impl = *impl_;
    const ModelConfig& cfg = impl.config;
    const int window = cfg.df_decoder_window_frames();
    const auto emb_count = static_cast<std::size_t>(window * cfg.erb_hidden());
    std::copy(emb_window, emb_window + emb_count, impl.df_emb.begin());
    // Input layout: [window][conv_ch][nb_df]; tensor layout: [conv_ch][window][nb_df].
    for (int w = 0; w < window; ++w) {
        for (int c = 0; c < cfg.conv_ch; ++c) {
            for (int f = 0; f < cfg.nb_df; ++f) {
                const std::size_t src =
                    (static_cast<std::size_t>(w) * static_cast<std::size_t>(cfg.conv_ch) +
                     static_cast<std::size_t>(c)) *
                        static_cast<std::size_t>(cfg.nb_df) +
                    static_cast<std::size_t>(f);
                const std::size_t dst =
                    (static_cast<std::size_t>(c) * static_cast<std::size_t>(window) +
                     static_cast<std::size_t>(w)) *
                        static_cast<std::size_t>(cfg.nb_df) +
                    static_cast<std::size_t>(f);
                impl.df_c0[dst] = c0_window[src];
            }
        }
    }
    if (state.df_decoder.empty()) {
        state.df_decoder = make_zero_state(impl.df_inputs);
    }

    std::vector<Ort::Value> inputs;
    inputs.reserve(impl.df_in_names.size());
    for (const std::string& name : impl.df_in_names) {
        if (name == "emb") {
            inputs.push_back(
                make_tensor(impl.df_emb, {1, window, static_cast<int64_t>(cfg.erb_hidden())}));
        } else if (name == "c0") {
            inputs.push_back(make_tensor(impl.df_c0, {1, cfg.conv_ch, window, cfg.nb_df}));
        } else {
            int index = 0;
            for (const std::string& state_name : state_names(impl.df_inputs)) {
                if (state_name == name) {
                    break;
                }
                ++index;
            }
            const std::vector<float>& value = state.df_decoder[static_cast<std::size_t>(index)];
            inputs.push_back(make_tensor(value, {1, 1, static_cast<int64_t>(value.size())}));
        }
    }

    auto outputs = impl.df_decoder->Run(Ort::RunOptions{nullptr},
                                        impl.df_in_ptrs.data(),
                                        inputs.data(),
                                        inputs.size(),
                                        impl.df_out_ptrs.data(),
                                        impl.df_out_ptrs.size());

    const std::vector<std::string> state_inputs = state_names(impl.df_inputs);
    std::vector<float> raw;
    for (std::size_t i = 0; i < impl.df_out_names.size(); ++i) {
        const std::string& name = impl.df_out_names[i];
        if (name == "coefs") {
            copy_output(outputs[i], raw);
        } else if (name.rfind("state_out_", 0) == 0) {
            for (std::size_t s = 0; s < state_inputs.size(); ++s) {
                if (name == state_output_name(state_inputs[s])) {
                    copy_output(outputs[i], state.df_decoder[s]);
                }
            }
        }
    }

    // The graph returns [frame][nb_df][df_order][2]; the DSP layer expects
    // [df_order][nb_df][2].
    const int nb_df = cfg.nb_df;
    const int order = cfg.df_order;
    impl.out_coefs.assign(static_cast<std::size_t>(order * nb_df * 2), 0.0f);
    for (int f = 0; f < nb_df; ++f) {
        for (int k = 0; k < order; ++k) {
            for (int c = 0; c < 2; ++c) {
                const std::size_t src =
                    (static_cast<std::size_t>(f) * order + static_cast<std::size_t>(k)) * 2 + c;
                const std::size_t dst =
                    (static_cast<std::size_t>(k) * nb_df + static_cast<std::size_t>(f)) * 2 + c;
                if (src < raw.size()) {
                    impl.out_coefs[dst] = raw[src];
                }
            }
        }
    }
    return impl.out_coefs;
}

}  // namespace dfn::enhancer
