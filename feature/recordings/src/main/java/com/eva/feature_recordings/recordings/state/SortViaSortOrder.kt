package com.eva.feature_recordings.recordings.state

internal fun <T, R> Iterable<T>.sortSelector(
	sortOrder: UISortOrder,
	selector: (T) -> Comparable<R>
): List<T> {
	return sortedWith { a, b ->
		when (sortOrder) {
			UISortOrder.ASC -> compareValuesBy(a, b, selector)
			UISortOrder.DESC -> compareValuesBy(b, a, selector)
		}
	}
}

internal fun sortedResults(
	recordings: List<SelectableRecordings>,
	sortInfo: UIRecordingSortInfo
): List<SelectableRecordings> = when (sortInfo.identifier) {
	UISortIdentifier.DATE_CREATED -> recordings.sortSelector(sortInfo.sortDirection) { it.recording.recordedAt }
	UISortIdentifier.DURATION -> recordings.sortSelector(sortInfo.sortDirection) { it.recording.duration }
	UISortIdentifier.NAME -> recordings.sortSelector(sortInfo.sortDirection) { it.recording.displayName }
	UISortIdentifier.SIZE -> recordings.sortSelector(sortInfo.sortDirection) { it.recording.sizeInBytes }
}