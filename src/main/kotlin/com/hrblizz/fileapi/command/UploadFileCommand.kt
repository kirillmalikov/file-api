package com.hrblizz.fileapi.command

import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileEntityRepository
import com.hrblizz.fileapi.service.FileStorage
import java.io.InputStream
import java.time.Instant
import org.springframework.stereotype.Component

@Component
class UploadFileCommand(
    private val fileStorage: FileStorage,
    private val fileEntityRepository: FileEntityRepository
) {

    fun execute(parameters: Parameters): String {
        val token = fileEntityRepository.save(
            FileEntity(
                filename = parameters.filename,
                contentType = parameters.contentType,
                meta = parameters.meta,
                source = parameters.source,
                expireTime = parameters.expireTime,
                size = parameters.size
            )
        ).token

        if(!fileStorage.storeFile(parameters.content, "$token.${parameters.filename}")) {
            fileEntityRepository.deleteByToken(token)
        }

        return token
    }

    data class Parameters(
        val filename: String,
        val contentType: String,
        val size: Long,
        val expireTime: Instant?,
        val meta: String?,
        val source: String,
        val content: InputStream
    )
}
