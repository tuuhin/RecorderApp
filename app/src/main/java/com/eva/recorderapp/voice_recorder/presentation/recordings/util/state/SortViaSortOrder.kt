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