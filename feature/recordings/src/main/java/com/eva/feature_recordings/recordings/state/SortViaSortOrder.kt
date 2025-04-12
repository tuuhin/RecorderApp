package com.eva.feature_recordings.recordings.state

internal fun <T, R> Iterable<T>.sortSelector(
	sortOrder: SortOrder,
	selector: (T) -> Comparable<R>
): List<T> {
	return sortedWith { a, b ->
		when (sortOrder) {
			SortOrder.ASC -> compareValuesBy(a, b, selector)
			SortOrder.DESC -> compareValuesBy(b, a, selector)
		}
	}
}

internal fun sortedResults(
	recordings: List<SelectableRecordings>,
	sortInfo: RecordingsSortInfo
): List<SelectableRecordings> = when (sortInfo.options) {
	SortOptions.DATE_CREATED -> recordings.sortSelector(sortInfo.order) { it.recording.recordedAt }
	SortOptions.DURATION -> recordings.sortSelector(sortInfo.order) { it.recording.duration }
	SortOptions.NAME -> recordings.sortSelector(sortInfo.order) { it.recording.displayName }
	SortOptions.SIZE -> recordings.sortSelector(sortInfo.order) { it.recording.sizeInBytes }
}