package com.eva.recorder.di

import com.eva.recorder.data.recorder.VoiceRecorderImpl
import com.eva.recorder.domain.recorder.AudioByteDataProvider
import com.eva.recorder.domain.recorder.VoiceRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
internal abstract class RecorderImplBindings {

    @Binds
    @ServiceScoped
    abstract fun providesAudioVoiceRecorder(impl: VoiceRecorderImpl): VoiceRecorder

    @Binds
    @ServiceScoped
    abstract fun providesAudioStreamSource(impl: VoiceRecorderImpl): AudioByteDataProvider
}
