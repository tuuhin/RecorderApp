#!/usr/bin/env bash

set -Eeuo pipefail

# Download an open audio clip and use ffmpeg to make it a 10-second max editor integration-test audio fixture.
# Usage: bash test_assets_builder.sh [--clean]

OUTPUT_DIR="${OUTPUT_DIR:-src/androidTest/assets}"
SAMPLE_RATE="${SAMPLE_RATE:-48000}"
CHANNELS="${CHANNELS:-1}"
DURATION="${DURATION:-10}"
AUDIO_URL="${AUDIO_URL:-https://upload.wikimedia.org/wikipedia/commons/8/8e/Drums.ogg}"
SOURCE="$OUTPUT_DIR/.editor_source.wav"

mkdir -p "$OUTPUT_DIR"

remove_temp_files() {
    rm -f "$SOURCE" "$OUTPUT_DIR"/.editor_*.tmp.* "$OUTPUT_DIR/.editor_download.tmp"
}
trap remove_temp_files EXIT

if [[ "${1:-}" == "--clean" ]]; then
    rm -f "$OUTPUT_DIR"/editor_*.{mp3,ogg,flac,wav} "$SOURCE"
    echo "Removed editor fixtures from $OUTPUT_DIR"
    exit 0
fi

for cmd in ffmpeg ffprobe awk curl; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
       echo "Missing dependency: $cmd" >&2
       exit 1
    fi
done

# Download open source audio clip and process it with ffmpeg to standard PCM16 WAV format (max 10 seconds)
TEMP_DOWNLOAD="$OUTPUT_DIR/.editor_download.tmp"
echo "Downloading source audio fixture..."
curl -fL -s -o "$TEMP_DOWNLOAD" "$AUDIO_URL"

ffmpeg -hide_banner -loglevel error -y \
    -i "$TEMP_DOWNLOAD" \
    -t "$DURATION" \
    -ac "$CHANNELS" -ar "$SAMPLE_RATE" -c:a pcm_s16le "$SOURCE"

rm -f "$TEMP_DOWNLOAD"

generate() {
    local name="$1" extension="$2" encoder="$3"
    shift 3
    local output="$OUTPUT_DIR/editor_${name}.${extension}"
    local temporary="$OUTPUT_DIR/.editor_${name}.tmp.${extension}"

    if [[ -f "$output" ]]; then
       echo "Skipping existing fixture: $output"
       return
    fi

    if ! ffmpeg -hide_banner -loglevel error -encoders | awk -v encoder="$encoder" '$0 ~ encoder { found=1 } END { exit !found }'; then
       echo "UNAVAILABLE: $name (FFmpeg encoder $encoder is not installed)" >&2
       return 0
    fi

    if ffmpeg -hide_banner -loglevel error -y -i "$SOURCE" -vn -c:a "$encoder" "$@" "$temporary"; then
       mv "$temporary" "$output"
       local actual
       actual="$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$output")"
       if ! awk -v actual="$actual" -v expected="$DURATION" 'BEGIN { delta=actual-expected; if (delta < 0) delta=-delta; exit !(delta <= 1) }'; then
          echo "Duration check failed for $output: $actual seconds" >&2
          return 1
       fi
       echo "Created $output ($actual seconds)"
    else
       rm -f "$temporary"
       echo "Failed to generate $name fixture" >&2
       return 1
    fi
}

generate mp3 mp3 libmp3lame -b:a 96k
generate ogg ogg vorbis -q:a 3 -strict experimental -ac 2
generate flac flac flac
generate wav wav pcm_s16le -ar "$SAMPLE_RATE" -ac "$CHANNELS"

echo "Editor fixtures generated in $OUTPUT_DIR"
