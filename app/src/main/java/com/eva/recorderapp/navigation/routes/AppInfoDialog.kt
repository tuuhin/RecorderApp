package com.eva.recorderapp.navigation.routes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.eva.recorderapp.BuildConfig
import com.eva.ui.R
import com.eva.ui.navigation.NavDialogs
import com.eva.ui.theme.DownloadableFonts
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.ApplicationInfo

fun NavGraphBuilder.appInfoDialog() = dialog<NavDialogs.ApplicationInfo> {
    AppDialogInfoContent()
}

@Composable
private fun AppDialogInfoContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Surface(
        modifier = modifier.widthIn(min = 280.dp, max = 340.dp),
        shape = AlertDialogDefaults.shape,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        color = AlertDialogDefaults.containerColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            WaveformBackdrop(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize(),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    modifier = modifier,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shadowElevation = 2.dp,
                ) {
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = DownloadableFonts.PLUS_CODE_LATIN_FONT_FAMILY,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                AboutLinkRow(
                    painter = painterResource(R.drawable.ic_code),
                    title = stringResource(R.string.app_info_source_code),
                    link = ApplicationInfo.GITHUB_PROJECT_LINK,
                    onClick = {
                        context.launchViewIntent(ApplicationInfo.GITHUB_PROJECT_LINK.toUri())
                    },
                )
                AboutLinkRow(
                    painter = painterResource(R.drawable.ic_author),
                    title = stringResource(R.string.app_info_author),
                    link = ApplicationInfo.PROJECT_AUTHOR_LINK,
                    onClick = {
                        context.launchViewIntent(ApplicationInfo.PROJECT_AUTHOR_LINK.toUri())
                    },
                )
            }
        }
    }
}


@Composable
private fun WaveformBackdrop(
    color: Color,
    modifier: Modifier = Modifier,
    barWidth: Dp = 6.dp,
    containerColor: Color = AlertDialogDefaults.containerColor,
) {
    val heights = remember {
        listOf(
            .25f, .40f, .65f, .35f, .85f, .55f, .30f, .70f, .95f, .50f,
            .35f, .75f, .45f, .90f, .60f, .30f, .55f, .80f, .40f, .25f,
        )
    }

    Spacer(
        modifier = modifier.drawWithCache {
            val fadeBrush = Brush.verticalGradient(
                colorStops = arrayOf(
                    .0f to color,
                    0.55f to color.copy(alpha = .2f),
                    1f to color.copy(alpha = 0f),
                ),
                startY = 0f,
                endY = size.height,
            )

            val fade = Brush.verticalGradient(
                colorStops = arrayOf(
                    .0f to containerColor,
                    1f to color.copy(alpha = .2f),
                ),
                startY = size.height,
                endY = 0f,
            )

            onDrawBehind {
                val barWidthPx = barWidth.toPx()
                val count = heights.size
                val gap = (size.width - barWidthPx * count) / (count - 1)

                heights.forEachIndexed { index, fraction ->
                    val barHeight = size.height * fraction * .25f
                    drawRoundRect(
                        fadeBrush,
                        topLeft = Offset(
                            x = index * (barWidthPx + gap),
                            y = (size.height - barHeight) / 2f - size.width * .25f,
                        ),
                        size = Size(barWidthPx, barHeight),
                        cornerRadius = CornerRadius(barWidthPx / 2f),
                    )
                }
                drawRect(fade)
            }
        },
    )
}

@Composable
private fun VersionPill(version: String, modifier: Modifier = Modifier) {

}

@Composable
private fun AboutLinkRow(
    painter: Painter,
    title: String,
    link: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val destination = remember(link) { link.toUri().host?.removePrefix("www.") ?: link }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = destination,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_open_link),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

private fun Context.launchViewIntent(uri: Uri) {
    try {
        Intent(Intent.ACTION_VIEW).apply {
            data = uri
            startActivity(this)
        }
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            applicationContext,
            R.string.cannot_launch_activity,
            Toast.LENGTH_SHORT,
        ).show()
    }
}

@PreviewLightDark
@Composable
private fun AppInfoDialogContentPreview() = RecorderAppTheme {
    AppDialogInfoContent()
}
