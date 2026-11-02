#!/usr/bin/env bash

set -Eeuo pipefail

# Audio test asset builder.
# Downloads one reusable public domain recording of at least thirty minutes
# and cuts it into MP3 samples of a few durations using FFmpeg.
#
# Usage:
#   bash test_assets_builder.sh          # download (if needed) and generate
#   bash test_assets_builder.sh --clean  # remove all generated assets

OUTPUT_DIR="${OUTPUT_DIR:-src/androidTest/assets}"
SAMPLE_RATE="${SAMPLE_RATE:-44100}"
CHANNELS="${CHANNELS:-2}"
BITRATE="${BITRATE:-128k}"
TARGET_DURATION=1800

SEARCH_API="https://archive.org/advancedsearch.php"
METADATA_API="https://archive.org/metadata"

DURATIONS=(
    20
    60
    300
    1800
)

mkdir -p "$OUTPUT_DIR"

remove_temp_files() {
    rm -f "$OUTPUT_DIR/.audio_source.tmp" "$OUTPUT_DIR"/sample_*.tmp.mp3
}

# Always drop partial downloads and partial encodes, even when a step fails.
trap remove_temp_files EXIT

clean_all() {
    rm -f "$OUTPUT_DIR"/sample_*.mp3 \
        "$OUTPUT_DIR/.audio_source" \
        "$OUTPUT_DIR/.audio_source_url"
    echo "Removed generated assets from $OUTPUT_DIR"
    exit 0
}

if [[ "${1:-}" == "--clean" ]]; then
    clean_all
fi

for cmd in curl jq ffmpeg ffprobe awk tr; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "Missing dependency: $cmd" >&2
        exit 1
    fi
done

# Case-insensitive license check compatible with macOS Bash 3.2.
is_reusable_license() {
    local license
    license="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"

    [[ "$license" == *"publicdomain/zero"* ]] ||
    [[ "$license" == *"creativecommons.org/publicdomain/zero"* ]] ||
    [[ "$license" == *"public domain"* ]]
}

format_duration() {
    local seconds="$1"

    if (( seconds < 60 )); then
        printf '%ss' "$seconds"
    elif (( seconds < 3600 )); then
        printf '%sm' "$((seconds / 60))"
    else
        printf '%sh' "$((seconds / 3600))"
    fi
}

# Convert duration metadata such as:
# 1800, 1800.5, 30:00, or 1:00:00
# into seconds. Return failure if the format is unknown.
parse_duration() {
    local value="$1"

    awk -v value="$value" '
        BEGIN {
            n = split(value, parts, ":")

            if (n == 1 && value ~ /^[0-9]+([.][0-9]+)?$/) {
                print int(value)
                exit 0
            }

            if (n == 2 &&
                parts[1] ~ /^[0-9]+$/ &&
                parts[2] ~ /^[0-9]+([.][0-9]+)?$/) {
                print int(parts[1] * 60 + parts[2])
                exit 0
            }

            if (n == 3 &&
                parts[1] ~ /^[0-9]+$/ &&
                parts[2] ~ /^[0-9]+$/ &&
                parts[3] ~ /^[0-9]+([.][0-9]+)?$/) {
                print int(parts[1] * 3600 + parts[2] * 60 + parts[3])
                exit 0
            }

            exit 1
        }
    '
}

# Download a single source recording lasting at least thirty minutes.
find_source_recording() {
    local source_file="$OUTPUT_DIR/.audio_source"
    local source_url_file="$OUTPUT_DIR/.audio_source_url"

    # Reuse an existing valid source recording.
    if [[ -f "$source_file" ]]; then
        local existing_duration

        existing_duration="$(
            ffprobe -v error \
                -show_entries format=duration \
                -of default=noprint_wrappers=1:nokey=1 \
                "$source_file" 2>/dev/null || true
        )"

        if [[ -n "$existing_duration" ]] &&
            awk -v actual="$existing_duration" \
                -v required="$TARGET_DURATION" \
                'BEGIN { exit !(actual >= required) }'; then
            echo "Reusing existing source recording."
            return 0
        fi

        rm -f "$source_file" "$source_url_file"
    fi

    echo "Searching Internet Archive for a reusable recording of at least thirty minutes..."

    local query='mediatype:audio AND (licenseurl:"http://creativecommons.org/publicdomain/zero/1.0/" OR licenseurl:"https://creativecommons.org/publicdomain/zero/1.0/")'

    local results

    if ! results="$(
        curl -fsSL --get "$SEARCH_API" \
            --data-urlencode "q=$query" \
            --data-urlencode "fl[]=identifier" \
            --data-urlencode "fl[]=title" \
            --data-urlencode "rows=100" \
            --data-urlencode "output=json"
    )"; then
        echo "Internet Archive search failed." >&2
        return 1
    fi

    local identifiers
    identifiers="$(jq -r '.response.docs[].identifier // empty' <<<"$results")"

    if [[ -z "$identifiers" ]]; then
        echo "No candidate recordings were found." >&2
        return 1
    fi

    while IFS= read -r identifier; do
        [[ -n "$identifier" ]] || continue

        echo "Inspecting: $identifier"

        local metadata

        if ! metadata="$(curl -fsSL "$METADATA_API/$identifier")"; then
            continue
        fi

        # Verify the actual item's license metadata.
        local license
        license="$(
            jq -r '
                [
                    .metadata.licenseurl?,
                    .metadata.rights?,
                    .metadata.license?
                ]
                | map(select(type == "string"))
                | join(" ")
            ' <<<"$metadata"
        )"

        if ! is_reusable_license "$license"; then
            continue
        fi

        # Inspect audio files and their advertised durations.
        local candidates
        candidates="$(
            jq -r '
                .files[]
                | select(."private" != "true")
                | select(."source" != "metadata")
                | select(."name" != null)
                | select(
                    ."format" == "WAVE" or
                    ."format" == "MP3" or
                    ."format" == "Ogg Vorbis" or
                    ."format" == "FLAC" or
                    ."format" == "MPEG Audio"
                )
                | [
                    ."name",
                    (."length" // "")
                ]
                | @tsv
            ' <<<"$metadata"
        )"

        while IFS="$(printf '\t')" read -r filename advertised_length; do
            [[ -n "$filename" ]] || continue

            local advertised_seconds

            if ! advertised_seconds="$(parse_duration "$advertised_length")"; then
                # Unknown metadata duration: skip rather than risk
                # downloading a very large unsuitable file.
                continue
            fi

            if (( advertised_seconds < TARGET_DURATION )); then
                continue
            fi

            local encoded_filename
            encoded_filename="$(
                jq -rn --arg value "$filename" '$value | @uri'
            )"

            local url="https://archive.org/download/$identifier/$encoded_filename"
            local temp_file="$source_file.tmp"

            rm -f "$temp_file"

            echo "Downloading source: $filename"
            echo "Advertised duration: ${advertised_seconds}s"

            if ! curl -fLsS --retry 2 "$url" -o "$temp_file"; then
                rm -f "$temp_file"
                continue
            fi

            local actual_duration

            actual_duration="$(
                ffprobe -v error \
                    -show_entries format=duration \
                    -of default=noprint_wrappers=1:nokey=1 \
                    "$temp_file" 2>/dev/null || true
            )"

            if [[ -z "$actual_duration" ]] ||
                ! awk -v actual="$actual_duration" \
                    -v required="$TARGET_DURATION" \
                    'BEGIN { exit !(actual >= required) }'; then
                rm -f "$temp_file"
                continue
            fi

            mv "$temp_file" "$source_file"
            printf '%s\n' "https://archive.org/details/$identifier" \
                > "$source_url_file"

            echo "Source recording downloaded successfully."
            echo "Duration: ${actual_duration}s"

            return 0

        done <<<"$candidates"

    done <<<"$identifiers"

    echo "Could not find a suitable thirty minute recording." >&2
    echo "Try another source or provide a recording manually." >&2
    return 1
}

# Cut each MP3 sample from the same source recording.
generate_samples() {
    local source_file="$OUTPUT_DIR/.audio_source"

    for duration in "${DURATIONS[@]}"; do
        local label
        label="$(format_duration "$duration")"

        local output="$OUTPUT_DIR/sample_${label}.mp3"

        if [[ -f "$output" ]]; then
            echo "Skipping existing file: $output"
            continue
        fi

        echo "Generating ${label} sample..."

        local temp_output="$output.tmp.mp3"

        rm -f "$temp_output"

        # only the audio stream goes into the mp3, embedded cover art
        # of the source is dropped
        if ffmpeg -hide_banner -loglevel error -y \
            -i "$source_file" \
            -t "$duration" \
            -vn \
            -map a:0 \
            -ar "$SAMPLE_RATE" \
            -ac "$CHANNELS" \
            -c:a libmp3lame \
            -b:a "$BITRATE" \
            "$temp_output"; then

            mv "$temp_output" "$output"
            echo "Created: $output"
        else
            rm -f "$temp_output"
            echo "Failed to generate ${label} sample." >&2
            return 1
        fi
    done
}

# The source recording is only needed while samples are generated,
# do not keep the large download around afterwards.
remove_source_recording() {
    rm -f "$OUTPUT_DIR/.audio_source" "$OUTPUT_DIR/.audio_source_url"
}

main() {
    if ! find_source_recording; then
        exit 1
    fi

    if [[ -f "$OUTPUT_DIR/.audio_source_url" ]]; then
        echo "Source:"
        cat "$OUTPUT_DIR/.audio_source_url"
    fi

    generate_samples
    remove_source_recording

    echo
    echo "========================================"
    echo "Audio test assets generated successfully"
    echo "Output directory: $OUTPUT_DIR"
    echo "========================================"

    echo
    echo "Generated files:"
    ls -lh "$OUTPUT_DIR"/sample_*.mp3
}

main "$@"
