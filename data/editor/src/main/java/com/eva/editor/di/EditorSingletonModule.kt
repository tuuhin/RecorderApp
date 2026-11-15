package com.eva.editor.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import com.eva.editor.data.EditedItemSaverImpl
import com.eva.editor.data.transformer.AudioTransformerImpl
import com.eva.editor.domain.AudioTransformer
import com.eva.editor.domain.EditedItemSaver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EditorSingletonModule {

	@Provides
	@Singleton
	fun providesEditorSaver(
		@ApplicationContext context: Context,
		settings: RecorderFileSettingsRepo
	): EditedItemSaver = EditedItemSaverImpl(context, settings)

    @Provides
    @Singleton
    @OptIn(UnstableApi::class)
    fun providesAudioTrimmer(
        @ApplicationContext context: Context,
    ): AudioTransformer = AudioTransformerImpl(context)
}
