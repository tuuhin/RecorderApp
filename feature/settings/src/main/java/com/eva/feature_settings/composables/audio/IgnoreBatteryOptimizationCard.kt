package com.eva.feature_settings.composables.audio

import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.eva.feature_settings.contract.IgnoreBatteryOptimizationContract
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun IgnoreBatteryOptimizationCard(
    onGranted: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
) {
    val context = LocalContext.current
    val currentOnGranted by rememberUpdatedState(onGranted)

    val launcher = rememberLauncherForActivityResult(
        contract = IgnoreBatteryOptimizationContract(),
        onResult = {
            val powerManager = context.getSystemService<PowerManager>()
            val isIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            currentOnGranted(isIgnored)
        },
    )

    Card(
        modifier = modifier,
        onClick = { launcher.launch(Unit) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(all = dimensionResource(id = R.dimen.card_padding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                    .background(
                        color = contentColor,
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_battery_slanted),
                    contentDescription = stringResource(id = R.string.turn_off_optimization),
                    tint = containerColor,
                )
            }
            Text(
                text = stringResource(id = R.string.turn_off_optimization),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )
        }
    }
}


@PreviewLightDark
@Composable
private fun IgnoreBatteryOptimizationCardPreview() = RecorderAppTheme {
    IgnoreBatteryOptimizationCard(onGranted = {})
}
