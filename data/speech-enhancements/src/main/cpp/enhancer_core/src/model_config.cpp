#include "model_config.h"

#include <algorithm>
#include <cctype>
#include <cmath>
#include <fstream>
#include <sstream>
#include <stdexcept>
#include <string>

#include "stft.h"

namespace dfn::enhancer {
namespace {

std::string trim(const std::string& value) {
    const auto first = value.find_first_not_of(" \t\r\n");
    if (first == std::string::npos) {
        return {};
    }
    const auto last = value.find_last_not_of(" \t\r\n");
    return value.substr(first, last - first + 1);
}

std::string lowercase(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) {
        return static_cast<char>(std::tolower(c));
    });
    return value;
}

float parse_float(const std::map<std::string, std::string>& values, const std::string& key,
                  float fallback) {
    const auto it = values.find(key);
    if (it == values.end()) {
        return fallback;
    }
    try {
        return std::stof(it->second);
    } catch (const std::exception&) {
        return fallback;
    }
}

int parse_int(const std::map<std::string, std::string>& values, const std::string& key,
              int fallback) {
    const auto it = values.find(key);
    if (it == values.end()) {
        return fallback;
    }
    try {
        return std::stoi(it->second);
    } catch (const std::exception&) {
        return fallback;
    }
}

std::string parse_string(const std::map<std::string, std::string>& values, const std::string& key,
                         const std::string& fallback) {
    const auto it = values.find(key);
    return it == values.end() ? fallback : it->second;
}

}  // namespace

float ModelConfig::compute_norm_alpha(int sample_rate, int hop_size, float tau) {
    if (sample_rate <= 0 || hop_size <= 0 || tau <= 0.0f) {
        return 0.99f;
    }
    const float dt = static_cast<float>(hop_size) / static_cast<float>(sample_rate);
    const float alpha = std::exp(-dt / tau);
    float a = 1.0f;
    int precision = 3;
    // libDF rounds to an increasing number of decimals until the factor is strictly below 1.
    while (a >= 1.0f) {
        const float scale = std::pow(10.0f, static_cast<float>(precision));
        a = std::round(alpha * scale) / scale;
        if (precision > 9) {
            break;
        }
        ++precision;
    }
    return a;
}

void ModelConfig::_apply_section(const std::string& section,
                                 const std::map<std::string, std::string>& values) {
    if (section == "df") {
        sample_rate = parse_int(values, "sr", sample_rate);
        fft_size = parse_int(values, "fft_size", fft_size);
        hop_size = parse_int(values, "hop_size", hop_size);
        nb_erb = parse_int(values, "nb_erb", nb_erb);
        nb_df = parse_int(values, "nb_df", nb_df);
        min_nb_erb_freqs = parse_int(values, "min_nb_erb_freqs", min_nb_erb_freqs);
        df_order = parse_int(values, "df_order", df_order);
        df_lookahead = parse_int(values, "df_lookahead", df_lookahead);
        norm_tau = parse_float(values, "norm_tau", norm_tau);
    } else if (section == "deepfilternet") {
        conv_lookahead = parse_int(values, "conv_lookahead", conv_lookahead);
        conv_ch = parse_int(values, "conv_ch", conv_ch);
        // Older exports stored the deep filter geometry in this section (config._fix_df).
        df_order = parse_int(values, "df_order", df_order);
        df_lookahead = parse_int(values, "df_lookahead", df_lookahead);
    } else if (section == "train") {
        model_type = parse_string(values, "model", model_type);
    }
}

ModelConfig ModelConfig::from_ini_string(const std::string& text) {
    ModelConfig config = deepfilternet3_default();
    std::istringstream stream(text);
    std::string line;
    std::string section;
    std::map<std::string, std::string> values;
    bool has_norm_alpha = false;
    float explicit_alpha = 0.0f;

    auto flush = [&]() {
        if (!section.empty()) {
            config._apply_section(section, values);
            if (section == "df") {
                const auto alpha = values.find("norm_alpha");
                if (alpha != values.end()) {
                    try {
                        explicit_alpha = std::stof(alpha->second);
                        has_norm_alpha = true;
                    } catch (const std::exception&) {
                        has_norm_alpha = false;
                    }
                }
            }
        }
        values.clear();
    };

    while (std::getline(stream, line)) {
        const std::string stripped = trim(line);
        if (stripped.empty() || stripped[0] == '#' || stripped[0] == ';') {
            continue;
        }
        if (stripped.front() == '[' && stripped.back() == ']') {
            flush();
            section = lowercase(trim(stripped.substr(1, stripped.size() - 2)));
            continue;
        }
        const auto equals = stripped.find('=');
        if (equals == std::string::npos || section.empty()) {
            continue;
        }
        const std::string key = lowercase(trim(stripped.substr(0, equals)));
        const std::string value = trim(stripped.substr(equals + 1));
        values[key] = value;
    }
    flush();

    config.norm_alpha =
        has_norm_alpha && explicit_alpha > 0.0f && explicit_alpha < 1.0f
            ? explicit_alpha
            : compute_norm_alpha(config.sample_rate, config.hop_size, config.norm_tau);
    return config;
}

ModelConfig ModelConfig::from_ini_file(const std::string& path, std::string* error) {
    std::ifstream file(path);
    if (!file.good()) {
        if (error != nullptr) {
            *error = "could not open model config: " + path;
        }
        return deepfilternet3_default();
    }
    std::ostringstream buffer;
    buffer << file.rdbuf();
    if (error != nullptr) {
        error->clear();
    }
    return from_ini_string(buffer.str());
}

ModelConfig ModelConfig::deepfilternet3_default() {
    ModelConfig config;
    config.sample_rate = 48000;
    config.fft_size = 960;
    config.hop_size = 480;
    config.nb_erb = 32;
    config.nb_df = 96;
    config.min_nb_erb_freqs = 2;
    config.df_order = 5;
    config.df_lookahead = 2;
    config.norm_tau = 1.0f;
    config.conv_lookahead = 2;
    config.conv_ch = 64;
    config.model_type = "deepfilternet3";
    config.norm_alpha = compute_norm_alpha(config.sample_rate, config.hop_size, config.norm_tau);
    return config;
}

void ModelConfig::validate() const {
    if (sample_rate != 48000)
        throw std::invalid_argument("DeepFilterNet 3 processes audio at 48 kHz");

    if (fft_size <= 0 || hop_size <= 0)
        throw std::invalid_argument("fft_size and hop_size must be positive");

    if (2 * hop_size > fft_size)
        throw std::invalid_argument("hop_size must not exceed fft_size / 2");

    if (fft_size % 32 != 0)
        throw std::invalid_argument("fft_size must be a multiple of 32 (PFFFT requirement)");

    if (nb_erb <= 0 || nb_erb % 4 != 0)
        throw std::invalid_argument("nb_erb must be a positive multiple of 4");

    if (nb_df <= 0 || nb_df >= freq_size())
        throw std::invalid_argument("nb_df must be in (0, freq_size)");

    if (df_order <= 0) throw std::invalid_argument("df_order must be positive");

    if (df_lookahead < 0 || conv_lookahead < 0)
        throw std::invalid_argument("lookahead values must be non-negative");

    if (df_lookahead > df_order)
        throw std::invalid_argument("df_lookahead must not exceed df_order");

    if (conv_ch <= 0) throw std::invalid_argument("conv_ch must be positive");

    if (model_type != "deepfilternet3")
        throw std::invalid_argument("unsupported model type: " + model_type +
                                    " (only deepfilternet3 is supported)");

    if (norm_alpha <= 0.0f || norm_alpha >= 1.0f)
        throw std::invalid_argument("norm_alpha must be in (0, 1)");
}

}  // namespace dfn::enhancer
