package com.eva.recorderapp.voice_recorder.presentation.recordings.util.state

fun <T, R> Iterable<T>.sortSelector(
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

fun sortedResults(
	recordings: List<SelectableRecordings>,
	sortInfo: RecordingsSortInfo
): List<SelectableRecordings> = when (sortInfo.options) {
	SortOptions.DATE_CREATED -> recordings.sortSelector(sortInfo.order) { it.recoding.recordedAt }
	SortOptions.DURATION -> recordings.sortSelector(sortInfo.order) { it.recoding.duration }
	SortOptions.NAME -> recordings.sortSelector(sortInfo.order) { it.recoding.displayName }
	SortOptions.SIZE -> recordings.sortSelector(sortInfo.order) { it.recoding.sizeInBytes }
}