#include "onnx_backend.h"

#include <nnapi_provider_factory.h>
#include <onnxruntime_cxx_api.h>

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <format>
#include <memory>
#include <ranges>
#include <span>
#include <stdexcept>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace dfn::enhancer {
namespace {

constexpr std::string_view kStateInPrefix = "state_in_";
constexpr std::string_view kStateOutPrefix = "state_out_";

Ort::Env& shared_env() {
    static Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "dfn3");
    return env;
}

Ort::MemoryInfo& memory_info() {
    static Ort::MemoryInfo info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    return info;
}

[[nodiscard]] constexpr bool is_dynamic(std::int64_t dim) noexcept {
    return dim < 0;
}

[[nodiscard]] const tensor* find_tensor(std::span<const tensor> specs,
                                        std::string_view name) noexcept {
    const auto it = std::ranges::find(specs, name, &tensor::name);
    return it == specs.end() ? nullptr : &(*it);
}

[[nodiscard]] bool expect_shape(std::span<const tensor> specs, std::string_view graph,
                                std::string_view name, std::span<const std::int64_t> expected,
                                std::string* error) {
    auto set_error = [&](std::string msg) {
        if (error != nullptr) {
            *error = std::move(msg);
        }
    };

    const tensor* spec = find_tensor(specs, name);
    if (spec == nullptr) {
        set_error(std::format("{} is missing tensor '{}'", graph, name));
        return false;
    }
    if (spec->element_type != ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT) {
        set_error(std::format("{} tensor '{}' is not float32", graph, name));
        return false;
    }
    if (spec->shape.size() != expected.size()) {
        set_error(std::format("{} tensor '{}' has rank {}, expected {}",
                              graph,
                              name,
                              spec->shape.size(),
                              expected.size()));
        return false;
    }

    for (const std::weakly_incrementable auto i :
         std::views::iota(std::size_t{0}, expected.size())) {
        if (expected[i] > 0 && !is_dynamic(spec->shape[i]) && spec->shape[i] != expected[i]) {
            set_error(std::format("{} tensor '{}' dimension {} is {}, expected {}",
                                  graph,
                                  name,
                                  i,
                                  spec->shape[i],
                                  expected[i]));
            return false;
        }
    }
    return true;
}

[[nodiscard]] std::vector<std::string> state_names(std::span<const tensor> specs) {
    std::vector<std::string> names;
    for (const tensor& spec : specs) {
        if (std::string_view{spec.name}.starts_with(kStateInPrefix)) {
            names.push_back(spec.name);
        }
    }
    std::ranges::sort(names);
    return names;
}

[[nodiscard]] std::string state_output_name(std::string_view input) {
    std::string_view suffix = input;
    if (suffix.starts_with(kStateInPrefix)) {
        suffix.remove_prefix(kStateInPrefix.size());
    }
    return std::format("{}{}", kStateOutPrefix, suffix);
}

[[nodiscard]] int hidden_size(const tensor& spec) noexcept {
    std::int64_t total = 1;
    for (const std::int64_t dim : spec.shape) {
        total *= dim > 0 ? dim : 1;
    }
    return static_cast<int>(total);
}

[[nodiscard]] std::vector<std::vector<float>> make_zero_state(std::span<const tensor> inputs) {
    std::vector<std::vector<float>> state;
    for (const tensor& spec : inputs) {
        if (std::string_view{spec.name}.starts_with(kStateInPrefix)) {
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
    const auto src = std::span{value.GetTensorData<float>(), count};
    std::ranges::copy(src, out.begin());
}

[[nodiscard]] Ort::Value make_tensor(std::span<const float> data,
                                     std::span<const std::int64_t> shape) {
    return Ort::Value::CreateTensor<float>(
        memory_info(), const_cast<float*>(data.data()), data.size(), shape.data(), shape.size());
}

void assign_ptrs(std::span<const std::string> names, std::vector<const char*>& ptrs) {
    ptrs.clear();
    ptrs.reserve(names.size());
    for (const std::string& name : names) {
        ptrs.push_back(name.c_str());
    }
}

// Collects the names and shapes of a session's inputs or outputs.
void describe_session(Ort::Session& session, std::vector<std::string>& names,
                      std::vector<tensor>& specs, bool inputs) {
    Ort::AllocatorWithDefaultOptions allocator;
    names.clear();
    specs.clear();
    const std::size_t count = inputs ? session.GetInputCount() : session.GetOutputCount();
    names.reserve(count);
    specs.reserve(count);

    for (const std::weakly_incrementable auto i : std::views::iota(std::size_t{0}, count)) {
        auto name = inputs ? session.GetInputNameAllocated(i, allocator)
                           : session.GetOutputNameAllocated(i, allocator);
        names.emplace_back(name.get());
        auto type_info = inputs ? session.GetInputTypeInfo(i) : session.GetOutputTypeInfo(i);
        tensor spec;
        spec.name = names.back();
        if (type_info.GetONNXType() == ONNX_TYPE_TENSOR) {
            auto tensor_info = type_info.GetTensorTypeAndShapeInfo();
            spec.shape = tensor_info.GetShape();
            spec.element_type = static_cast<std::int32_t>(tensor_info.GetElementType());
        }
        specs.push_back(std::move(spec));
    }
}

// Precomputed classification of a session's inputs/outputs so the per-frame run
// loops avoid string comparisons and repeated re-sorting of the state names.
enum class io_role : std::uint8_t {
    none,
    feat_erb,
    feat_spec,
    emb,
    e0,
    e1,
    e2,
    e3,
    c0,
    lsnr,
    mask,
    coefs,
    state,
};

struct io_binding {
    io_role role = io_role::none;
    std::size_t state_index = 0;
};

[[nodiscard]] constexpr io_role classify_input(std::string_view name) noexcept {
    if (name == "feat_erb") return io_role::feat_erb;
    if (name == "feat_spec") return io_role::feat_spec;
    if (name == "emb") return io_role::emb;
    if (name == "e0") return io_role::e0;
    if (name == "e1") return io_role::e1;
    if (name == "e2") return io_role::e2;
    if (name == "e3") return io_role::e3;
    if (name == "c0") return io_role::c0;
    if (name.starts_with(kStateInPrefix)) return io_role::state;
    return io_role::none;
}

[[nodiscard]] constexpr io_role classify_output(std::string_view name) noexcept {
    if (name == "e0") return io_role::e0;
    if (name == "e1") return io_role::e1;
    if (name == "e2") return io_role::e2;
    if (name == "e3") return io_role::e3;
    if (name == "emb") return io_role::emb;
    if (name == "c0") return io_role::c0;
    if (name == "lsnr") return io_role::lsnr;
    if (name == "m") return io_role::mask;
    if (name == "coefs") return io_role::coefs;
    if (name.starts_with(kStateOutPrefix)) return io_role::state;
    return io_role::none;
}

// Maps each declared input/output of a session to a role. Recurrent edges are
// resolved against `state_names` (the sorted state_in_* names), mirroring the
// mapping the run loops used to compute inline on every frame.
[[nodiscard]] std::vector<io_binding> build_bindings(const std::vector<std::string>& names,
                                                     const std::vector<std::string>& state_names,
                                                     bool inputs) {
    std::vector<io_binding> bindings;
    bindings.reserve(names.size());

    for (const std::string& name : names) {
        io_binding binding;
        binding.role = inputs ? classify_input(name) : classify_output(name);

        if (binding.role == io_role::state) {
            std::size_t index = state_names.size();
            if (inputs) {
                const auto it = std::ranges::find(state_names, name);
                index = it == state_names.end()
                            ? state_names.size()
                            : static_cast<std::size_t>(std::distance(state_names.begin(), it));
            } else {
                for (const std::weakly_incrementable auto s :
                     std::views::iota(std::size_t{0}, state_names.size())) {
                    if (state_output_name(state_names[s]) == name) {
                        index = s;
                        break;
                    }
                }
            }
            if (index == state_names.size())
                binding.role = io_role::none;
            else
                binding.state_index = index;
        }
        bindings.push_back(binding);
    }
    return bindings;
}

}  // namespace

struct onnx_backend_runner::Impl {
    model_config config;

    std::unique_ptr<Ort::Session> encoder;
    std::unique_ptr<Ort::Session> erb_decoder;
    std::unique_ptr<Ort::Session> df_decoder;

    std::vector<tensor> enc_inputs, enc_outputs;
    std::vector<tensor> erb_inputs, erb_outputs;
    std::vector<tensor> df_inputs, df_outputs;

    std::vector<std::string> enc_in_names, enc_out_names;
    std::vector<std::string> erb_in_names, erb_out_names;
    std::vector<std::string> df_in_names, df_out_names;
    std::vector<const char*> enc_in_ptrs, enc_out_ptrs;
    std::vector<const char*> erb_in_ptrs, erb_out_ptrs, df_in_ptrs, df_out_ptrs;

    std::vector<float> enc_erb, enc_spec;
    std::vector<float> erb_emb, erb_e0, erb_e1, erb_e2, erb_e3;
    std::vector<float> df_emb, df_c0;
    std::vector<float> out_mask, out_coefs;

    // Precomputed once in init() and reused for every frame.
    std::vector<std::string> enc_state_names, erb_state_names, df_state_names;
    std::vector<io_binding> enc_in_bindings, enc_out_bindings;
    std::vector<io_binding> erb_in_bindings, erb_out_bindings;
    std::vector<io_binding> df_in_bindings, df_out_bindings;
    std::vector<Ort::Value> enc_inputs_buf, erb_inputs_buf, df_inputs_buf;
};

onnx_backend_runner::onnx_backend_runner() : impl_(std::make_unique<Impl>()) {}

onnx_backend_runner::~onnx_backend_runner() = default;

const std::vector<tensor>& onnx_backend_runner::encoder_inputs() const noexcept {
    return impl_->enc_inputs;
}
const std::vector<tensor>& onnx_backend_runner::encoder_outputs() const noexcept {
    return impl_->enc_outputs;
}
const std::vector<tensor>& onnx_backend_runner::erb_decoder_inputs() const noexcept {
    return impl_->erb_inputs;
}
const std::vector<tensor>& onnx_backend_runner::df_decoder_inputs() const noexcept {
    return impl_->df_inputs;
}

std::unique_ptr<onnx_backend_runner> onnx_backend_runner::from_files(
    std::string_view encoder_path, std::string_view erb_decoder_path,
    std::string_view df_decoder_path, const model_config& config, Options options,
    std::string* error) {
    auto backend = std::unique_ptr<onnx_backend_runner>(new onnx_backend_runner());
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
        const std::string enc_str{encoder_path};
        const std::string erb_str{erb_decoder_path};
        const std::string df_str{df_decoder_path};

        backend->impl_->encoder =
            std::make_unique<Ort::Session>(shared_env(), enc_str.c_str(), session_options);
        backend->impl_->erb_decoder =
            std::make_unique<Ort::Session>(shared_env(), erb_str.c_str(), session_options);
        backend->impl_->df_decoder =
            std::make_unique<Ort::Session>(shared_env(), df_str.c_str(), session_options);
    } catch (const Ort::Exception& exception) {
        if (error != nullptr) {
            *error = std::format("failed to load ONNX model: {}", exception.what());
        }
        return nullptr;
    }
    if (!backend->init(config, error)) {
        return nullptr;
    }
    return backend;
}

std::unique_ptr<onnx_backend_runner> onnx_backend_runner::from_memory(
    std::span<const std::uint8_t> encoder, std::span<const std::uint8_t> erb_decoder,
    std::span<const std::uint8_t> df_decoder, const model_config& config, Options options,
    std::string* error) {
    auto backend = std::unique_ptr<onnx_backend_runner>(new onnx_backend_runner());
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
            *error = std::format("failed to load ONNX model from memory: {}", exception.what());
        }
        return nullptr;
    }
    if (!backend->init(config, error)) {
        return nullptr;
    }
    return backend;
}

bool onnx_backend_runner::init(const model_config& config, std::string* error) {
    std::string message;
    impl_->config = config;
    Impl& impl = *impl_;

    describe_session(*impl.encoder, impl.enc_in_names, impl.enc_inputs, true);
    describe_session(*impl.encoder, impl.enc_out_names, impl.enc_outputs, false);
    describe_session(*impl.erb_decoder, impl.erb_in_names, impl.erb_inputs, true);
    describe_session(*impl.erb_decoder, impl.erb_out_names, impl.erb_outputs, false);
    describe_session(*impl.df_decoder, impl.df_in_names, impl.df_inputs, true);
    describe_session(*impl.df_decoder, impl.df_out_names, impl.df_outputs, false);

    const std::int64_t ch = config.conv_ch;
    const std::int64_t nb_erb = config.nb_erb;
    const std::int64_t nb_df = config.nb_df;
    const std::int64_t hidden = config.erb_hidden();
    const std::int64_t window = config.encoder_window_frames();
    const std::int64_t df_window = dfn::enhancer::model_config::df_decoder_window_frames();

    auto fail = [&](std::string what) {
        if (error != nullptr) {
            *error = std::move(what);
        }
        return false;
    };

    // Encoder.
    if (!expect_shape(impl.enc_inputs,
                      "enc",
                      "feat_erb",
                      std::array<std::int64_t, 4>{1, 1, window, nb_erb},
                      &message) ||
        !expect_shape(impl.enc_inputs,
                      "enc",
                      "feat_spec",
                      std::array<std::int64_t, 4>{1, 2, window, nb_df},
                      &message) ||
        !expect_shape(impl.enc_outputs,
                      "enc",
                      "e0",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_erb},
                      &message) ||
        !expect_shape(impl.enc_outputs,
                      "enc",
                      "e1",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_erb / 2},
                      &message) ||
        !expect_shape(impl.enc_outputs,
                      "enc",
                      "e2",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_erb / 4},
                      &message) ||
        !expect_shape(impl.enc_outputs,
                      "enc",
                      "e3",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_erb / 4},
                      &message) ||
        !expect_shape(
            impl.enc_outputs, "enc", "emb", std::array<std::int64_t, 3>{1, 1, hidden}, &message) ||
        !expect_shape(impl.enc_outputs,
                      "enc",
                      "c0",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_df},
                      &message) ||
        !expect_shape(
            impl.enc_outputs, "enc", "lsnr", std::array<std::int64_t, 3>{1, 1, 1}, &message)) {
        return fail(std::move(message));
    }

    // ERB decoder.
    if (!expect_shape(impl.erb_inputs,
                      "erb_dec",
                      "emb",
                      std::array<std::int64_t, 3>{1, 1, hidden},
                      &message) ||
        !expect_shape(impl.erb_inputs,
                      "erb_dec",
                      "e0",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_erb},
                      &message) ||
        !expect_shape(impl.erb_inputs,
                      "erb_dec",
                      "e1",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_erb / 2},
                      &message) ||
        !expect_shape(impl.erb_inputs,
                      "erb_dec",
                      "e2",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_erb / 4},
                      &message) ||
        !expect_shape(impl.erb_inputs,
                      "erb_dec",
                      "e3",
                      std::array<std::int64_t, 4>{1, ch, 1, nb_erb / 4},
                      &message) ||
        !expect_shape(impl.erb_outputs,
                      "erb_dec",
                      "m",
                      std::array<std::int64_t, 4>{1, 1, 1, nb_erb},
                      &message)) {
        return fail(std::move(message));
    }

    // DF decoder.
    if (!expect_shape(impl.df_inputs,
                      "df_dec",
                      "emb",
                      std::array<std::int64_t, 3>{1, df_window, hidden},
                      &message) ||
        !expect_shape(impl.df_inputs,
                      "df_dec",
                      "c0",
                      std::array<std::int64_t, 4>{1, ch, df_window, nb_df},
                      &message) ||
        !expect_shape(impl.df_outputs,
                      "df_dec",
                      "coefs",
                      std::array<std::int64_t, 4>{1, 1, nb_df, 2 * config.df_order},
                      &message)) {
        return fail(std::move(message));
    }

    // Recurrent state inputs: [1, 1, H] each, paired with a state_out_<i> output.
    auto validate_state = [&](std::span<const tensor> inputs,
                              std::span<const tensor>
                                  outputs,
                              std::string_view graph,
                              std::string_view expected_output) -> bool {
        const std::vector<std::string> names = state_names(inputs);
        if (names.empty()) {
            return fail(std::format("{} has no recurrent state inputs (state_in_*)", graph));
        }
        for (const std::string& name : names) {
            if (!expect_shape(
                    inputs, graph, name, std::array<std::int64_t, 3>{1, 1, -1}, &message)) {
                return false;
            }
            const std::string out_name = state_output_name(name);
            if (find_tensor(outputs, out_name) == nullptr) {
                return fail(std::format("{} is missing output '{}'", graph, out_name));
            }
        }
        if (find_tensor(outputs, expected_output) == nullptr) {
            return fail(std::format("{} is missing output '{}'", graph, expected_output));
        }
        return true;
    };

    if (!validate_state(impl.enc_inputs, impl.enc_outputs, "enc", "lsnr") ||
        !validate_state(impl.erb_inputs, impl.erb_outputs, "erb_dec", "m") ||
        !validate_state(impl.df_inputs, impl.df_outputs, "df_dec", "coefs")) {
        return fail(std::move(message));
    }

    assign_ptrs(impl.enc_in_names, impl.enc_in_ptrs);
    assign_ptrs(impl.enc_out_names, impl.enc_out_ptrs);
    assign_ptrs(impl.erb_in_names, impl.erb_in_ptrs);
    assign_ptrs(impl.erb_out_names, impl.erb_out_ptrs);
    assign_ptrs(impl.df_in_names, impl.df_in_ptrs);
    assign_ptrs(impl.df_out_names, impl.df_out_ptrs);

    // Resolve the recurrent state names and edge roles once, so the per-frame
    // run loops neither re-sort names nor compare strings on the hot path.
    impl.enc_state_names = state_names(impl.enc_inputs);
    impl.erb_state_names = state_names(impl.erb_inputs);
    impl.df_state_names = state_names(impl.df_inputs);

    impl.enc_in_bindings = build_bindings(impl.enc_in_names, impl.enc_state_names, true);
    impl.enc_out_bindings = build_bindings(impl.enc_out_names, impl.enc_state_names, false);
    impl.erb_in_bindings = build_bindings(impl.erb_in_names, impl.erb_state_names, true);
    impl.erb_out_bindings = build_bindings(impl.erb_out_names, impl.erb_state_names, false);
    impl.df_in_bindings = build_bindings(impl.df_in_names, impl.df_state_names, true);
    impl.df_out_bindings = build_bindings(impl.df_out_names, impl.df_state_names, false);

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

void onnx_backend_runner::init_state(recurrent_state& state) const {
    state.encoder = make_zero_state(impl_->enc_inputs);
    state.erb_decoder = make_zero_state(impl_->erb_inputs);
    state.df_decoder = make_zero_state(impl_->df_inputs);
}

encoder_result onnx_backend_runner::run_encoder(std::span<const float> feat_erb, int window_frames,
                                                std::span<const float> feat_spec,
                                                recurrent_state& state) {
    Impl& impl = *impl_;
    const model_config& cfg = impl.config;
    const auto win = static_cast<std::size_t>(window_frames);
    const auto nb_df = static_cast<std::size_t>(cfg.nb_df);
    const auto erb_count = win * static_cast<std::size_t>(cfg.nb_erb);

    std::ranges::copy(feat_erb.first(erb_count), impl.enc_erb.begin());

    const auto spec_in = feat_spec.first(2 * win * nb_df);
    // Input layout: [window][2][nb_df]; tensor layout: [2][window][nb_df].
    for (const std::weakly_incrementable auto w : std::views::iota(std::size_t{0}, win)) {
        for (const std::weakly_incrementable auto channel :
             std::views::iota(std::size_t{0}, std::size_t{2})) {
            for (const std::weakly_incrementable auto f : std::views::iota(std::size_t{0}, nb_df)) {
                const std::size_t src = (w * 2 + channel) * nb_df + f;
                const std::size_t dst = (channel * win + w) * nb_df + f;
                impl.enc_spec[dst] = spec_in[src];
            }
        }
    }

    if (state.encoder.empty()) {
        state.encoder = make_zero_state(impl.enc_inputs);
    }

    const std::array<std::int64_t, 4> erb_shape{1, 1, window_frames, cfg.nb_erb};
    const std::array<std::int64_t, 4> spec_shape{1, 2, window_frames, cfg.nb_df};

    std::vector<Ort::Value>& inputs = impl.enc_inputs_buf;
    inputs.clear();
    inputs.reserve(impl.enc_in_names.size());
    for (const io_binding& binding : impl.enc_in_bindings) {
        switch (binding.role) {
            case io_role::feat_erb:
                inputs.push_back(make_tensor(impl.enc_erb, erb_shape));
                break;
            case io_role::feat_spec:
                inputs.push_back(make_tensor(impl.enc_spec, spec_shape));
                break;
            case io_role::state: {
                const std::vector<float>& value = state.encoder[binding.state_index];
                const std::array<std::int64_t, 3> state_shape{
                    1, 1, static_cast<std::int64_t>(value.size())};
                inputs.push_back(make_tensor(value, state_shape));
                break;
            }
            default:
                throw std::runtime_error("unexpected encoder input edge");
        }
    }

    auto outputs = impl.encoder->Run(Ort::RunOptions{nullptr},
                                     impl.enc_in_ptrs.data(),
                                     inputs.data(),
                                     inputs.size(),
                                     impl.enc_out_ptrs.data(),
                                     impl.enc_out_ptrs.size());

    encoder_result result;
    for (const std::weakly_incrementable auto i :
         std::views::iota(std::size_t{0}, impl.enc_out_names.size())) {
        switch (impl.enc_out_bindings[i].role) {
            case io_role::e0:
                copy_output(outputs[i], result.e0);
                break;
            case io_role::e1:
                copy_output(outputs[i], result.e1);
                break;
            case io_role::e2:
                copy_output(outputs[i], result.e2);
                break;
            case io_role::e3:
                copy_output(outputs[i], result.e3);
                break;
            case io_role::emb:
                copy_output(outputs[i], result.emb);
                break;
            case io_role::c0:
                copy_output(outputs[i], result.c0);
                break;
            case io_role::lsnr: {
                const auto info = outputs[i].GetTensorTypeAndShapeInfo();
                result.lsnr = outputs[i].GetTensorData<float>()[info.GetElementCount() - 1];
                break;
            }
            case io_role::state:
                copy_output(outputs[i], state.encoder[impl.enc_out_bindings[i].state_index]);
                break;
            default:
                break;
        }
    }
    return result;
}

const std::vector<float>& onnx_backend_runner::run_erb_decoder(const encoder_result& encoder,
                                                               recurrent_state& state) {
    Impl& impl = *impl_;
    const model_config& cfg = impl.config;
    if (state.erb_decoder.empty()) {
        state.erb_decoder = make_zero_state(impl.erb_inputs);
    }

    std::ranges::copy(encoder.emb, impl.erb_emb.begin());
    std::ranges::copy(encoder.e0, impl.erb_e0.begin());
    std::ranges::copy(encoder.e1, impl.erb_e1.begin());
    std::ranges::copy(encoder.e2, impl.erb_e2.begin());
    std::ranges::copy(encoder.e3, impl.erb_e3.begin());

    const std::int64_t ch = cfg.conv_ch;
    const std::array<std::int64_t, 3> emb_shape{
        1, 1, static_cast<std::int64_t>(impl.erb_emb.size())};
    const std::array<std::int64_t, 4> e0_shape{1, ch, 1, cfg.nb_erb};
    const std::array<std::int64_t, 4> e1_shape{1, ch, 1, cfg.nb_erb / 2};
    const std::array<std::int64_t, 4> e2_shape{1, ch, 1, cfg.nb_erb / 4};
    const std::array<std::int64_t, 4> e3_shape{1, ch, 1, cfg.nb_erb / 4};

    std::vector<Ort::Value>& inputs = impl.erb_inputs_buf;
    inputs.clear();
    inputs.reserve(impl.erb_in_names.size());
    for (const io_binding& binding : impl.erb_in_bindings) {
        switch (binding.role) {
            case io_role::emb:
                inputs.push_back(make_tensor(impl.erb_emb, emb_shape));
                break;
            case io_role::e0:
                inputs.push_back(make_tensor(impl.erb_e0, e0_shape));
                break;
            case io_role::e1:
                inputs.push_back(make_tensor(impl.erb_e1, e1_shape));
                break;
            case io_role::e2:
                inputs.push_back(make_tensor(impl.erb_e2, e2_shape));
                break;
            case io_role::e3:
                inputs.push_back(make_tensor(impl.erb_e3, e3_shape));
                break;
            case io_role::state: {
                const std::vector<float>& value = state.erb_decoder[binding.state_index];
                const std::array<std::int64_t, 3> state_shape{
                    1, 1, static_cast<std::int64_t>(value.size())};
                inputs.push_back(make_tensor(value, state_shape));
                break;
            }
            default:
                throw std::runtime_error("unexpected erb decoder input edge");
        }
    }
    auto outputs = impl.erb_decoder->Run(Ort::RunOptions{nullptr},
                                         impl.erb_in_ptrs.data(),
                                         inputs.data(),
                                         inputs.size(),
                                         impl.erb_out_ptrs.data(),
                                         impl.erb_out_ptrs.size());

    for (const std::weakly_incrementable auto i :
         std::views::iota(std::size_t{0}, impl.erb_out_names.size())) {
        switch (impl.erb_out_bindings[i].role) {
            case io_role::mask:
                copy_output(outputs[i], impl.out_mask);
                break;
            case io_role::state:
                copy_output(outputs[i], state.erb_decoder[impl.erb_out_bindings[i].state_index]);
                break;
            default:
                break;
        }
    }
    return impl.out_mask;
}

const std::vector<float>& onnx_backend_runner::run_df_decoder(std::span<const float> emb_window,
                                                              std::span<const float> c0_window,
                                                              recurrent_state& state) {
    Impl& impl = *impl_;
    const model_config& cfg = impl.config;
    const int window = dfn::enhancer::model_config::df_decoder_window_frames();
    const auto win = static_cast<std::size_t>(window);
    const auto ch = static_cast<std::size_t>(cfg.conv_ch);
    const auto nb_df = static_cast<std::size_t>(cfg.nb_df);
    const auto emb_hidden = static_cast<std::size_t>(cfg.erb_hidden());

    std::ranges::copy(emb_window.first(win * emb_hidden), impl.df_emb.begin());

    const auto c0_in = c0_window.first(win * ch * nb_df);
    // Input layout: [window][conv_ch][nb_df]; tensor layout: [conv_ch][window][nb_df].
    for (const std::weakly_incrementable auto w : std::views::iota(std::size_t{0}, win)) {
        for (const std::weakly_incrementable auto c : std::views::iota(std::size_t{0}, ch)) {
            for (const std::weakly_incrementable auto f : std::views::iota(std::size_t{0}, nb_df)) {
                const std::size_t src = (w * ch + c) * nb_df + f;
                const std::size_t dst = (c * win + w) * nb_df + f;
                impl.df_c0[dst] = c0_in[src];
            }
        }
    }

    if (state.df_decoder.empty()) {
        state.df_decoder = make_zero_state(impl.df_inputs);
    }

    const std::array<std::int64_t, 3> emb_shape{1, window, static_cast<std::int64_t>(emb_hidden)};
    const std::array<std::int64_t, 4> c0_shape{1, cfg.conv_ch, window, cfg.nb_df};

    std::vector<Ort::Value>& inputs = impl.df_inputs_buf;
    inputs.clear();
    inputs.reserve(impl.df_in_names.size());
    for (const io_binding& binding : impl.df_in_bindings) {
        switch (binding.role) {
            case io_role::emb:
                inputs.push_back(make_tensor(impl.df_emb, emb_shape));
                break;
            case io_role::c0:
                inputs.push_back(make_tensor(impl.df_c0, c0_shape));
                break;
            case io_role::state: {
                const std::vector<float>& value = state.df_decoder[binding.state_index];
                const std::array<std::int64_t, 3> state_shape{
                    1, 1, static_cast<std::int64_t>(value.size())};
                inputs.push_back(make_tensor(value, state_shape));
                break;
            }
            default:
                throw std::runtime_error("unexpected df decoder input edge");
        }
    }

    auto outputs = impl.df_decoder->Run(Ort::RunOptions{nullptr},
                                        impl.df_in_ptrs.data(),
                                        inputs.data(),
                                        inputs.size(),
                                        impl.df_out_ptrs.data(),
                                        impl.df_out_ptrs.size());

    std::vector<float> raw;
    for (const std::weakly_incrementable auto i :
         std::views::iota(std::size_t{0}, impl.df_out_names.size())) {
        switch (impl.df_out_bindings[i].role) {
            case io_role::coefs:
                copy_output(outputs[i], raw);
                break;
            case io_role::state:
                copy_output(outputs[i], state.df_decoder[impl.df_out_bindings[i].state_index]);
                break;
            default:
                break;
        }
    }

    // The graph returns [frame][nb_df][df_order][2]; the DSP layer expects
    // [df_order][nb_df][2].
    const auto order = static_cast<std::size_t>(cfg.df_order);
    impl.out_coefs.assign(order * nb_df * 2, 0.0f);
    for (const std::weakly_incrementable auto f : std::views::iota(std::size_t{0}, nb_df)) {
        for (const std::weakly_incrementable auto k : std::views::iota(std::size_t{0}, order)) {
            for (const std::weakly_incrementable auto c :
                 std::views::iota(std::size_t{0}, std::size_t{2})) {
                const std::size_t src = (f * order + k) * 2 + c;
                const std::size_t dst = (k * nb_df + f) * 2 + c;
                if (src < raw.size()) {
                    impl.out_coefs[dst] = raw[src];
                }
            }
        }
    }
    return impl.out_coefs;
}

}  // namespace dfn::enhancer
