package com.eva.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.eva.ui.R

private val provider = GoogleFont.Provider(
	providerAuthority = "com.google.android.gms.fonts",
	providerPackage = "com.google.android.gms",
	certificates = R.array.com_google_android_gms_fonts_certs
)

private val REDDIT_MONO = GoogleFont("M PLUS Code Latin")
private val NOVA_MONO = GoogleFont("Nova Mono")

object DownloadableFonts {
	val CLOCK_FACE = FontFamily(
		Font(
			googleFont = REDDIT_MONO,
			fontProvider = provider,
			weight = FontWeight.Medium,
		)
	)

	val NOVA_MONO_FONT_FAMILY = FontFamily(
		Font(
			googleFont = NOVA_MONO,
			fontProvider = provider,
			weight = FontWeight.Medium
		)
	)
}

