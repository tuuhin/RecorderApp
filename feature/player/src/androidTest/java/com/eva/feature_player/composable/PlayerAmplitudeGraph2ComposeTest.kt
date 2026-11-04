package com.eva.feature_player.composable

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eva.feature_player.views.PlayerAmplitudeGraph2View
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.graphics.Color as AndroidColor

class PlayerAmplitudeGraph2ComposeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun keepScreenOn() {
        compose.runOnUiThread {
            compose.activity.setTurnScreenOn(true)
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Test
    fun givenInitialAndChangedWaveformsWhenComposedThenTextureDisplaysCurrentData() {
        var values by mutableStateOf(FloatArray(100) { 1f })
        var ratio by mutableFloatStateOf(0.5f)
        compose.setContent {
            MaterialTheme {
                PlayerAmplitudeGraph2(
                    totalTrackDuration = 10.seconds,
                    trackPlayRatio = { ratio },
                    graphData = { values },
                    modifier = Modifier.size(300.dp, 150.dp),
                    plotColor = Color.Red,
                    timelineColor = Color.Transparent,
                    timelineColorVariant = Color.Transparent,
                    timelineTextColor = Color.Transparent,
                    trackPointerColor = Color.Transparent,
                    containerColor = Color.Black,
                    timelineTextStyle = TextStyle(fontSize = 12.sp),
                )
            }
        }

        val graph = compose.runOnIdle { findGraph(compose.activity.window.decorView) }
        assertNotNull(graph)
        val view = checkNotNull(graph)
        fun pixel(): Int? {
            var color: Int? = null
            compose.runOnUiThread {
                view.bitmap?.let { bitmap ->
                    color = bitmap.getPixel(bitmap.width / 2, bitmap.height / 4)
                    bitmap.recycle()
                }
            }
            return color
        }

        fun isRed(color: Int) = AndroidColor.red(color) > 128 && AndroidColor.green(color) < 64

        compose.waitUntil(10_000) { pixel()?.let(::isRed) == true }
        compose.runOnIdle {
            values = FloatArray(100) { if (it < 50) 1f else 0f }
            ratio = 0.75f
        }
        compose.waitUntil(10_000) { pixel()?.let { !isRed(it) } == true }
    }

    private fun findGraph(view: View): PlayerAmplitudeGraph2View? {
        if (view is PlayerAmplitudeGraph2View) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) findGraph(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
