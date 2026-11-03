package com.eva.worker.domain

import kotlin.uuid.Uuid

sealed interface WorkResource {

    val modelId: String?
    val workId: Uuid

    data class Enqueued(
        override val workId: Uuid,
        override val modelId: String? = null,
    ) : WorkResource

    data class Running(
        override val modelId: String? = null,
        override val workId: Uuid,
        val message: String,
        val progress: Int? = null,
    ) : WorkResource

    data class Success(
        override val workId: Uuid,
        override val modelId: String? = null,
        val message: String? = null,
    ) : WorkResource

    data class Failed(
        override val workId: Uuid,
        override val modelId: String? = null,
        val reason: String,
        val error: Throwable? = null,
    ) : WorkResource

    data class Canceled(
        override val workId: Uuid,
        override val modelId: String? = null,
        val reason: String,
    ) : WorkResource

    data class Blocked(
        override val workId: Uuid,
        override val modelId: String? = null,
    ) : WorkResource
}
