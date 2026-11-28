#ifndef DFN_ENHANCER_ENHANCER_CONFIG_H_
#define DFN_ENHANCER_ENHANCER_CONFIG_H_

// Runtime options of the enhancement engine. These are execution concerns only; model
// geometry lives in ModelConfig.

namespace dfn::enhancer {

// Stage selection computed from the local SNR estimate.
enum class EnhancementStage {
    kSilence,   // lsnr < min_db_thresh: apply a zero mask
    kBypass,    // lsnr > max_db_erb_thresh: leave the spectrum untouched
    kErbOnly,   // lsnr > max_db_df_thresh: apply ERB gains only
    kErbAndDf,  // apply ERB gains and deep filtering
};

struct EnhancerOptions {
    // Post-filter (Valin et al.). A beta of 0 disables it, matching the Rust runtime
    // default. The Python CLI enables it with beta = 0.02.
    float post_filter_beta = 0.0f;

    // Noise attenuation limit in dB. Values >= 100 disable the limit.
    float attenuation_limit_db = 100.0f;

    // Local SNR thresholds of the stage selection (libDF defaults).
    float min_db_thresh = -10.0f;
    float max_db_erb_thresh = 30.0f;
    float max_db_df_thresh = 20.0f;

    // Stage switches, mirroring the `run_erb` / `run_df` model flags.
    bool enable_erb_stage = true;
    bool enable_df_stage = true;

    // Inference threading.
    int intra_op_threads = 1;
    int inter_op_threads = 1;
    bool use_nnapi = false;
};

}  // namespace dfn::enhancer

#endif  // DFN_ENHANCER_ENHANCER_CONFIG_H_
