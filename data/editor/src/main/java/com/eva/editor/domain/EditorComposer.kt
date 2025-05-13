package com.eva.editor.domain

import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import kotlin.time.Duration

object EditorComposer {

	private fun applyCut(segments: AudioConfigsList, cut: AudioClipConfig): AudioConfigsList {
		return segments.flatMap { seg ->
			when {
				// No overlap with the segment, keep it
				cut.end <= seg.start || cut.start >= seg.end -> listOf(seg)

				// Cut completely removes the segment
				cut.start <= seg.start && cut.end >= seg.end -> emptyList()

				// Cut splits the segment in two: before cut, and after cut
				cut.start > seg.start && cut.end < seg.end -> listOf(
					AudioClipConfig(seg.start, cut.start), // Part before the cut
					AudioClipConfig(cut.end, seg.end)      // Part after the cut
				)

				// Cut removes the beginning of the segment
				cut.start <= seg.start -> listOf(AudioClipConfig(cut.end, seg.end))

				// Cut removes the end of the segment
				else -> listOf(AudioClipConfig(seg.start, cut.start))
			}
		}.filter { it.start < it.end }
	}

	private fun applyCrop(segments: AudioConfigsList, crop: AudioClipConfig): AudioConfigsList {
		return segments.mapNotNull { seg ->
			val start = maxOf(seg.start, crop.start)
			val end = minOf(seg.end, crop.end)
			if (start < end) AudioClipConfig(start, end) else null
		}
	}

	private fun mapLogicalToReal(
		logical: AudioClipConfig,
		segments: AudioConfigsList
	): AudioClipConfig {
		val ranges = mutableListOf<Pair<Duration, AudioClipConfig>>()

		var logicalCursor = Duration.ZERO
		for (seg in segments) {
			val length = seg.end - seg.start
			ranges.add(logicalCursor to seg)
			logicalCursor += length
		}

		fun logicalToReal(time: Duration): Duration {
			for ((logicalStart, realSeg) in ranges) {
				val realLength = realSeg.end - realSeg.start
				val logicalEnd = logicalStart + realLength
				if (time in logicalStart..<logicalEnd) {
					val offset = time - logicalStart
					return realSeg.start + offset
				}
			}
			// If not in range, clamp to end
			return segments.lastOrNull()?.end ?: Duration.ZERO
		}

		val realStart = logicalToReal(logical.start)
		val realEnd = logicalToReal(logical.end)

		return AudioClipConfig(realStart, realEnd)
	}

	fun applyLogicalEditSequence(totalDuration: Duration, edits: AudioConfigToActionList)
			: AudioConfigsList {
		// Start with full timeline: [0s, totalDuration]
		var current = listOf(AudioClipConfig(Duration.ZERO, totalDuration))

		edits.forEach { (editLogical, action) ->
			// Map logical edit range to real (original) timeline
			val mappedEdit = mapLogicalToReal(editLogical, current)

			current = when (action) {
				AudioEditAction.CROP -> applyCrop(current, mappedEdit)
				AudioEditAction.CUT -> applyCut(current, mappedEdit)
			}
		}

		return current
	}
}