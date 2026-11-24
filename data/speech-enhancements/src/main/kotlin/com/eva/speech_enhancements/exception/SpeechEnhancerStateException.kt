package com.eva.speech_enhancements.exception

internal class SpeechEnhancerStateException :
    IllegalStateException("Enhance was called again without reset; call reset() between enhance calls")
