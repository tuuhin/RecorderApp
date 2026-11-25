#!/usr/bin/env bash
#
# Populates src/androidTest/assets for the speech-enhancements instrumentation tests:
#
#   assets/model/{config.ini,enc.onnx,erb_dec.onnx,df_dec.onnx}   streaming graphs
#   assets/test_audio/p234_0NN.wav                                noisy Valentini clips
#   assets/clear_audio/p234_0NN.wav                               clean Valentini clips
#
# The DeepFilterNet 3 graphs published on Hugging Face are batch exports and cannot be
# driven frame by frame (their GRU state resets on every Run() and their causal
# convolutions zero-pad the time axis). They are downloaded to a temp folder and converted
# to streaming graphs with onnx-tooling/prepare_models.py, which runs inside a throwaway
# `uv` environment (onnx-tooling/.venv) -- no system Python, no Kaggle account.
#
# Usage:
#   ./build_tests_assets.sh [--force]
#
#   --force   re-download and re-convert everything, even if the assets already exist

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLING="$ROOT/onnx-tooling"
ASSETS="$ROOT/src/androidTest/assets"
MODEL_DIR="$ASSETS/model"
NOISY_DIR="$ASSETS/test_audio"
CLEAN_DIR="$ASSETS/clear_audio"

HF_BASE="https://huggingface.co/bitsydarel/deepfilternet3-onnx/resolve/main"
HF_FILES=(enc.onnx erb_dec.onnx df_dec.onnx config.ini)

KAGGLE_DS="https://www.kaggle.com/api/v1/datasets/download/muhmagdy/valentini-noisy"
# p234 exists only in the 56-speaker split; every index here was confirmed present.
CLIPS=(001 002 003 005 007 008 010 012 017 023)
NOISY_SPLIT="noisy_trainset_56spk_wav"
CLEAN_SPLIT="clean_trainset_56spk_wav"

FORCE=0
for arg in "$@"; do
	case "$arg" in
	--force) FORCE=1 ;;
	-h | --help)
		awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "${BASH_SOURCE[0]}"
		exit 0
		;;
	*)
		echo "unknown argument: $arg" >&2
		exit 2
		;;
	esac
done

log() { printf '%s\n' "$*"; }
die() {
	printf 'error: %s\n' "$*" >&2
	exit 1
}

# ---------------------------------------------------------------- preflight

command -v curl >/dev/null 2>&1 || die "curl is required but not on PATH"

if ! command -v uv >/dev/null 2>&1; then
	die "uv is required to build the Python environment but was not found.
     install it with:  brew install uv   (or:  curl -LsSf https://astral.sh/uv/install.sh | sh)"
fi

[ -f "$TOOLING/prepare_models.py" ] || die "missing $TOOLING/prepare_models.py"

TMP="$(mktemp -d "${TMPDIR:-/tmp}/speech-enhance-assets.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

# ---------------------------------------------------------------- helpers

# url dest
fetch() {
	local url="$1" dest="$2"
	curl -fsSL --retry 3 --retry-delay 2 -o "$dest.tmp" "$url"
	mv "$dest.tmp" "$dest"
}

# url dest -- skips work when the destination already exists and --force was not given
fetch_unless_present() {
	local url="$1" dest="$2"
	if [ -f "$dest" ] && [ "$FORCE" -eq 0 ]; then
		log "  keep   ${dest#"$ROOT"/}"
		return 0
	fi
	mkdir -p "$(dirname "$dest")"
	fetch "$url" "$dest"
	log "  fetch  ${dest#"$ROOT"/}"
}

# file -- asserts a 48 kHz mono PCM16 RIFF/WAVE container
verify_wav() {
	local file="$1"
	[ -s "$file" ] || die "missing or empty WAV: $file"
	[ "$(head -c 4 "$file")" = "RIFF" ] || die "not a RIFF/WAVE file: $file"

	local channels sample_rate
	channels="$(od -An -tu2 -j22 -N2 "$file" | tr -d ' ')"
	sample_rate="$(od -An -tu4 -j24 -N4 "$file" | tr -d ' ')"
	[ "$sample_rate" = "48000" ] || die "expected 48 kHz, got ${sample_rate} Hz in $file"
	[ "$channels" = "1" ] || die "expected mono, got ${channels} channel(s) in $file"
}

needs_model() {
	[ "$FORCE" -eq 1 ] && return 0
	local name
	for name in "${HF_FILES[@]}"; do
		[ -f "$MODEL_DIR/$name" ] || return 0
	done
	return 1
}

needs_audio() {
	[ "$FORCE" -eq 1 ] && return 0
	local clip
	for clip in "${CLIPS[@]}"; do
		[ -f "$NOISY_DIR/p234_$clip.wav" ] || return 0
		[ -f "$CLEAN_DIR/p234_$clip.wav" ] || return 0
	done
	return 1
}

# ---------------------------------------------------------------- models

build_models() {
	local raw="$TMP/raw" prepared="$TMP/prepared"
	mkdir -p "$raw" "$prepared"

	log "Downloading DeepFilterNet 3 graphs from Hugging Face"
	local name
	for name in "${HF_FILES[@]}"; do
		log "  fetch  $name"
		fetch "$HF_BASE/$name?download=true" "$raw/$name"
	done

	log "Creating the Python environment with uv (onnx-tooling/.venv)"
	if [ "$FORCE" -eq 1 ]; then
		uv venv "$TOOLING/.venv" --python 3.12 --clear --quiet
	else
		uv venv "$TOOLING/.venv" --python 3.12 --allow-existing --quiet
	fi
	uv pip install --python "$TOOLING/.venv/bin/python" \
		-r "$TOOLING/requirements.txt" --quiet

	log "Converting batch graphs to streaming graphs"
	"$TOOLING/.venv/bin/python" "$TOOLING/prepare_models.py" \
		--model-dir "$raw" --out-dir "$prepared"

	mkdir -p "$MODEL_DIR"
	install -m 0644 "$prepared/enc_stream.onnx" "$MODEL_DIR/enc.onnx"
	install -m 0644 "$prepared/erb_dec_stream.onnx" "$MODEL_DIR/erb_dec.onnx"
	install -m 0644 "$prepared/df_dec_stream.onnx" "$MODEL_DIR/df_dec.onnx"
	install -m 0644 "$raw/config.ini" "$MODEL_DIR/config.ini"
	log "  wrote  ${MODEL_DIR#"$ROOT"/}/{config.ini,enc.onnx,erb_dec.onnx,df_dec.onnx}"
}

# ---------------------------------------------------------------- audio

# split clip dest_dir
fetch_clip() {
	local split="$1" clip="$2" dest_dir="$3"
	local rel="$split/p234_$clip.wav"
	local encoded="${rel//\//%2F}"
	fetch_unless_present "$KAGGLE_DS?fileName=$encoded" "$dest_dir/p234_$clip.wav"
}

build_audio() {
	log "Downloading Valentini ${#CLIPS[@]}-clip set (noisy + clean)"
	local clip
	for clip in "${CLIPS[@]}"; do
		fetch_clip "$NOISY_SPLIT" "$clip" "$NOISY_DIR"
	done
	for clip in "${CLIPS[@]}"; do
		fetch_clip "$CLEAN_SPLIT" "$clip" "$CLEAN_DIR"
	done
}

# ---------------------------------------------------------------- main

mkdir -p "$MODEL_DIR" "$NOISY_DIR" "$CLEAN_DIR"

if needs_model; then
	build_models
else
	log "Models already present (use --force to rebuild)"
fi

if needs_audio; then
	build_audio
else
	log "Audio clips already present (use --force to re-download)"
fi

log "Verifying WAV assets"
for clip in "${CLIPS[@]}"; do
	verify_wav "$NOISY_DIR/p234_$clip.wav"
	verify_wav "$CLEAN_DIR/p234_$clip.wav"
done

log ""
log "Done. Assets in ${ASSETS#"$ROOT"/}:"
log "  model/       ${#HF_FILES[@]} files"
log "  test_audio/  ${#CLIPS[@]} noisy clips"
log "  clear_audio/ ${#CLIPS[@]} clean clips"
log ""
log "Run the tests with:"
log "  ./gradlew :data:speech-enhancements:connectedDebugAndroidTest"
