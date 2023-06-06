package com.hrblizz.fileapi.service

import com.hrblizz.fileapi.controller.exception.FileApiException
import com.hrblizz.fileapi.controller.exception.NotFoundException
import com.hrblizz.fileapi.library.log.ExceptionLogItem
import com.hrblizz.fileapi.library.log.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class LocalFileStorage(
    @Value("\${localstorage.directory}") private val directory: String,
    private val log: Logger
) : FileStorage {
    override fun storeFile(inputStream: InputStream, key: String): Boolean {
        return try {
            File(directory).mkdirs()

            val path = Paths.get(directory, key)

            FileOutputStream(path.toFile()).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            true
        } catch (e: Exception) {
            log.error(ExceptionLogItem("Error occurred while storing the file $key", e))

            throw FileApiException("Error occurred while storing the file")
        }
    }

    override fun getFile(key: String): File {
        val file: File = try {
            Paths.get(directory, key).toFile()
        } catch (e: Exception) {
            log.error(ExceptionLogItem("Error occurred while getting the file $key", e))

            throw FileApiException("Error occurred while getting the file")
        }

        if (!file.exists()) {
            throw NotFoundException("File was not found in storage")
        }

        return file
    }

    override fun deleteFile(key: String): Boolean {
        val files = File(directory).listFiles { _, fileName -> fileName.startsWith(key) }

        if (files != null && files.isNotEmpty()) {
            try {
                return files[0].delete()
            } catch (e: Exception) {
                log.error(ExceptionLogItem("Error occurred during the delete operation of file $key", e))

                throw FileApiException("Error occurred during the delete operation")
            }
        } else {
            throw NotFoundException("File was not found in storage")
        }
    }
}
