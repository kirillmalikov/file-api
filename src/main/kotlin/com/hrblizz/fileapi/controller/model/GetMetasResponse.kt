package com.hrblizz.fileapi.controller.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema
data class GetMetasResponse(
    val files: Map<String, FileMetaResponse>
)

@Schema
data class FileMetaResponse(
    val token: UUID,
    val fileName: String,
    val size: Long,
    val contentType: String,
    val createTime: Instant,
    val meta: String?
)
