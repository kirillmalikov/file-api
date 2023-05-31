package com.hrblizz.fileapi.data.entities

import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Id

data class FileEntity(
    @Id
    var token: String = UUID.randomUUID().toString(),
    val filename: String,
    val size: Long,
    val contentType: String,
    val createTime: Instant = Instant.now(),
    val expireTime: Instant? = null,
    val meta: String?,
    val source: String,
    val content: String
)
