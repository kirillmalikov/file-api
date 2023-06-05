package com.hrblizz.fileapi.data.entities

import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class FileEntity(
    @Id
    var token: String = UUID.randomUUID().toString(),
    val filename: String,
    val size: Long,
    val contentType: String,
    val createTime: Instant = Instant.now(),
    val expireTime: Instant? = null,
    val meta: String?,
    val source: String
)
