package com.hrblizz.fileapi.command

import com.hrblizz.fileapi.controller.exception.NotFoundException
import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileEntityRepository
import com.hrblizz.fileapi.library.log.Logger
import com.hrblizz.fileapi.service.FileStorage
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Paths
import org.springframework.stereotype.Component

@Component
class DownloadFileCommand(
    private val fileStorage: FileStorage,
    private val fileEntityRepository: FileEntityRepository
) {

    fun execute(token: String): Pair<FileEntity, InputStream> {
        val entity = fileEntityRepository.findByToken(token)
            ?: throw NotFoundException("File document with token = $token was not found")

        return entity to FileInputStream(fileStorage.getFile("$token.${entity.filename}"))
    }
}
