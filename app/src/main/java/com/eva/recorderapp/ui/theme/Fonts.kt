package com.eva.recorderapp.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.eva.recorderapp.R

private val provider = GoogleFont.Provider(
	providerAuthority = "com.google.android.gms.fonts",
	providerPackage = "com.google.android.gms",
	certificates = R.array.com_google_android_gms_fonts_certs
)

private val ROBOTO_CONDENSED = GoogleFont("Roboto Condensed")

object DownloadableFonts{
	val CLOCK_FACE = FontFamily(
		Font(googleFont = ROBOTO_CONDENSED, fontProvider = provider, weight = FontWeight.SemiBold)
	)
}

