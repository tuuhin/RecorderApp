package com.eva.speech_enhancements.metrics

data class EnhancementScore(
    val clip: String,
    val siSdrBeforeDb: Double,
    val siSdrAfterDb: Double,
    val deltaSiSdrDb: Double,
    val correlationBefore: Double,
    val correlationAfter: Double,
    val lsdBeforeDb: Double,
    val lsdAfterDb: Double,
    val segSnrBeforeDb: Double,
    val segSnrAfterDb: Double,
    val noiseFloorGainDb: Double,
    val bandRetention300To3400Db: Double,
    val bandRetention300To8000Db: Double,
    val alignmentLagSamples: Int,
) {
    val deltaLsdDb: Double get() = lsdAfterDb - lsdBeforeDb
    val deltaSegSnrDb: Double get() = segSnrAfterDb - segSnrBeforeDb
    val deltaCorrelation: Double get() = correlationAfter - correlationBefore

    /** One logcat-friendly line, printed before any assertion so a failed gate still reports. */
    fun toMeasureLine(): String = "MEASURE quality clip=$clip " +
        "siSdrBefore=%.2f siSdrAfter=%.2f deltaSiSdr=%.2f ".format(siSdrBeforeDb, siSdrAfterDb, deltaSiSdrDb) +
        "corrBefore=%.4f corrAfter=%.4f deltaCorr=%.4f ".format(
            correlationBefore,
            correlationAfter,
            deltaCorrelation,
        ) +
        "lsdBefore=%.2f lsdAfter=%.2f ".format(lsdBeforeDb, lsdAfterDb) +
        "segSnrBefore=%.2f segSnrAfter=%.2f ".format(segSnrBeforeDb, segSnrAfterDb) +
        "noiseFloorGain=%.2f ".format(noiseFloorGainDb) +
        "band3400=%.2f band8000=%.2f ".format(bandRetention300To3400Db, bandRetention300To8000Db) +
        "lag=%d".format(alignmentLagSamples)
}
