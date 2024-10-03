package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.bookmarks.AudioBookmarkModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioBookmarksList(
	bookmarks: ImmutableList<AudioBookmarkModel>,
	onEditBookMark: (AudioBookmarkModel) -> Unit,
	onDeleteBookMark: (AudioBookmarkModel) -> Unit,
	modifier: Modifier = Modifier,
	onExportBookmarks: () -> Unit = {},
	contentPadding: PaddingValues = PaddingValues(),
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val keys: ((Int, AudioBookmarkModel) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.bookMarkId }
	}

	val contentType: ((Int, AudioBookmarkModel) -> Any?) = remember {
		{ _, _ -> AudioBookmarkModel::class.simpleName }
	}

	val canExportBookmarks by remember(bookmarks) {
		derivedStateOf { bookmarks.isNotEmpty() }
	}

	Column(
		modifier = modifier
			.padding(contentPadding)
			.sizeIn(minHeight = dimensionResource(R.dimen.bottom_sheet_min_height)),
		verticalArrangement = Arrangement.spacedBy(2.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = stringResource(R.string.bookmarks_list_title),
				style = MaterialTheme.typography.titleLarge,
				color = MaterialTheme.colorScheme.primary,
				fontWeight = FontWeight.Bold,
			)
			AnimatedVisibility(
				visible = canExportBookmarks,
			) {
				TooltipBox(
					positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
					tooltip = {
						PlainTooltip {
							Text(text = stringResource(R.string.bookmark_action_export))
						}
					},
					state = rememberTooltipState()
				) {
					IconButton(
						onClick = onExportBookmarks,
						colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
					) {
						Icon(
							painter = painterResource(R.drawable.ic_export),
							contentDescription = stringResource(R.string.bookmark_action_export)
						)
					}
				}
			}
		}
		Crossfade(
			targetState = canExportBookmarks,
			label = "Bookmarks empty ot fill animation",
			modifier = modifier,
			animationSpec = tween(
				durationMillis = 600,
				delayMillis = 100,
				easing = FastOutSlowInEasing
			)
		) { isNotEmpty ->
			if (isNotEmpty) {
				LazyColumn(
					verticalArrangement = Arrangement.spacedBy(4.dp),
				) {
					itemsIndexed(
						items = bookmarks,
						key = keys,
						contentType = contentType
					) { _, bookmark ->
						AudioBookMarkCard(
							bookmark = bookmark,
							onDelete = { onDeleteBookMark(bookmark) },
							onEdit = { onEditBookMark(bookmark) },
							modifier = Modifier
								.fillMaxWidth()
								.animateItem()
						)
					}
				}
			}
			else NoBookmarksPlaceHolder()
		}
	}
}

@Composable
private fun NoBookmarksPlaceHolder(modifier: Modifier = Modifier) {
	Column(
		verticalArrangement = Arrangement.spacedBy(6.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifier
	) {
		Image(
			painter = painterResource(R.drawable.ic_bookmark),
			contentDescription = stringResource(R.string.no_bookmarks_found),
			colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary),
			modifier = Modifier.size(64.dp)
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = stringResource(R.string.no_bookmarks_found),
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Text(
			text = stringResource(R.string.no_bookmarks_found_desc),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
	}
}

private class BooksMarksListPreviewParams :
	CollectionPreviewParameterProvider<ImmutableList<AudioBookmarkModel>>(
		listOf(
			PreviewFakes.FAKE_BOOKMARKS_LIST,
			persistentListOf()
		)
	)

@PreviewLightDark
@Composable
private fun AudioBookMarksList(
	@PreviewParameter(BooksMarksListPreviewParams::class)
	bookmarks: ImmutableList<AudioBookmarkModel>,
) = RecorderAppTheme {
	Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
		AudioBookmarksList(
			bookmarks = bookmarks,
			onDeleteBookMark = {},
			onEditBookMark = {},
			contentPadding = PaddingValues(12.dp)
		)
	}
}