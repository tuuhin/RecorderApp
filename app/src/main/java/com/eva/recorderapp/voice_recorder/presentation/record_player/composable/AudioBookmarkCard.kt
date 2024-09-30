package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.bookmarks.AudioBookmarkModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.datetime.format

@Composable
fun AudioBookMarkCard(
	bookmark: AudioBookmarkModel,
	onDelete: () -> Unit,
	onEdit: () -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(2.dp),
	actionColors: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
	val timeText = remember {
		bookmark.timeStamp.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS)
	}

	val bookmarkText = bookmark.text.ifBlank { stringResource(R.string.enter_your_bookmark) }

	Row(
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalAlignment = Alignment.CenterVertically,
		modifier = modifier.padding(contentPadding),
	) {
		Badge(
			containerColor = MaterialTheme.colorScheme.tertiaryContainer,
			contentColor = MaterialTheme.colorScheme.onTertiaryContainer
		) {
			Text(
				text = timeText,
				style = MaterialTheme.typography.labelMedium,
				fontWeight = FontWeight.SemiBold,
				modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
			)
		}
		Box(
			modifier = Modifier
				.height(IntrinsicSize.Max)
				.clickable(onClick = onEdit, role = Role.Button)
				.weight(1f)
		) {
			Text(
				text = bookmarkText,
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				fontStyle = FontStyle.Italic,
				modifier = Modifier
					.padding(vertical = 6.dp, horizontal = 4.dp)

			)

		}
		IconButton(
			onClick = onDelete,
			colors = IconButtonDefaults.iconButtonColors(contentColor = actionColors)
		) {
			Icon(
				painter = painterResource(R.drawable.ic_minus),
				contentDescription = stringResource(R.string.delete_bookmark)
			)
		}

	}
}

@PreviewLightDark
@Composable
private fun AudioBookmarkCardPreview() = RecorderAppTheme {
	Surface {
		AudioBookMarkCard(
			bookmark = PreviewFakes.FAKE_BOOKMARK_MODEL,
			onEdit = {},
			onDelete = {}
		)
	}
}