package com.eva.speech_enhancements.exception

internal class SpeechEnhancementHandleException :
    IllegalStateException("Speech enhancer handle was released, create a new instance to continue")
