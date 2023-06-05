package com.hrblizz.fileapi.command

import com.hrblizz.fileapi.controller.exception.FileApiException
import com.hrblizz.fileapi.controller.exception.NotFoundException
import com.hrblizz.fileapi.data.repository.FileEntityRepository
import com.hrblizz.fileapi.library.log.ExceptionLogItem
import com.hrblizz.fileapi.library.log.Logger
import com.hrblizz.fileapi.service.FileStorage
import org.springframework.stereotype.Component

@Component
class DeleteFileCommand(
    private val log: Logger,
    private val fileStorage: FileStorage,
    private val fileEntityRepository: FileEntityRepository
) {

    fun execute(token: String) {
        if (fileStorage.deleteFile(token)) {
            val deleted = try {
                fileEntityRepository.deleteByToken(token)
            } catch (e: Exception) {
                this.log.error(ExceptionLogItem("Unexpected error while deleting a file with token: $token", e))

                throw FileApiException("Error while deleting a file")
            }
            if (deleted != 1L) {
                throw NotFoundException("File document with token = $token was not found")
            }
        }
    }
}
