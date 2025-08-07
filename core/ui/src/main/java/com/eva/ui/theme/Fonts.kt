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

private val PLUS_CODE_LATIN = GoogleFont("M PLUS Code Latin")
private val NOVA_MONO = GoogleFont("Nova Mono")
private val SPLINE_SANS_MONO = GoogleFont("Spline Sans Mono")
private val FIRA_SANS = GoogleFont("Fira Sans")

object DownloadableFonts {
	val PLUS_CODE_LATIN_FONT_FAMILY = FontFamily(
		Font(
			googleFont = PLUS_CODE_LATIN,
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
	val SPLINE_SANS_MONO_FONT_FAMILY = FontFamily(
		Font(
			googleFont = SPLINE_SANS_MONO,
			fontProvider = provider,
			weight = FontWeight.Medium
		)
	)

	val FIRA_SANS_FONT_FAMILY = FontFamily(
		Font(
			googleFont = FIRA_SANS,
			fontProvider = provider,
			weight = FontWeight.Medium
		)
	)
}

