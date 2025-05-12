package com.eva.feature_editor.undoredo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

class UndoRedoManager<T>(private val maxSize: Int) {

	private val _allActionsStack = MutableStateFlow<List<T>>(emptyList())
	val allActions: StateFlow<List<T>> = _allActionsStack.asStateFlow()

	private val _undoStack = MutableStateFlow<List<T>>(emptyList())
	private val _redoStack = MutableStateFlow<List<T>>(emptyList())

	fun add(state: T) {
		val allActions = _allActionsStack.updateAndGet { it + state }
		_undoStack.update { allActions.takeLast(maxSize) }
		_redoStack.update { emptyList() }
	}


	fun undo(): List<T> {
		val newList = _undoStack.updateAndGet { stack ->
			if (stack.isEmpty()) return@updateAndGet stack
			val last = stack.last()
			// add the last element to redo stack
			_redoStack.update { (it + last).take(maxSize) }
			stack.dropLast(1)
		}

		return _allActionsStack.updateAndGet { newList }
	}


	fun redo(): List<T> {
		_redoStack.update { stack ->
			if (stack.isEmpty()) return@update stack
			// last element of redo stack
			val lastUndoneState = stack.first()
			_undoStack.update { it + lastUndoneState }
			stack.drop(1)
		}
		val undoStackValue = _undoStack.value.lastOrNull()


		// Update allActions to reflect the redo
		return _allActionsStack.updateAndGet { allActions ->
			undoStackValue?.let { allActions + it } ?: allActions
		}
	}


	fun clearHistory() {
		_undoStack.update { emptyList() }
		_redoStack.update { emptyList() }
	}

	val undoRedoState = combine(
		flow = _undoStack.map { it.isNotEmpty() },
		flow2 = _redoStack.map { it.isNotEmpty() },
		transform = ::UndoRedoState
	).distinctUntilChanged()
}