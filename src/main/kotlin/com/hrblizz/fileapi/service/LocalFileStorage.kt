package com.hrblizz.fileapi.service

import com.hrblizz.fileapi.controller.exception.FileApiException
import com.hrblizz.fileapi.controller.exception.NotFoundException
import com.hrblizz.fileapi.library.log.ExceptionLogItem
import com.hrblizz.fileapi.library.log.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths
import org.springframework.stereotype.Component

const val FILES_STORAGE_DIRECTORY = "/files-api/storage/files/"

@Component
class LocalFileStorage(private val log: Logger) : FileStorage {
    override fun storeFile(inputStream: InputStream, key: String): Boolean {
        return try {
            File(FILES_STORAGE_DIRECTORY).mkdirs()

            val path = Paths.get(FILES_STORAGE_DIRECTORY, key)

            FileOutputStream(path.toFile()).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            true
        } catch (e: Exception) {
            log.error(ExceptionLogItem("", e))

            throw FileApiException("")
        }
    }

    override fun getFile(key: String): File {
        val file: File = try {
            Paths.get(FILES_STORAGE_DIRECTORY, key).toFile()
        } catch (e: Exception) {
            log.error(ExceptionLogItem("", e))

            throw FileApiException("")
        }

        if (!file.exists()) {
            throw NotFoundException("File with key = $key was not found in storage")
        }

        return file
    }

    override fun deleteFile(key: String): Boolean {
        val files = File(FILES_STORAGE_DIRECTORY).listFiles { _, fileName -> fileName.startsWith(key) }

        if (files != null && files.isNotEmpty()) {
            try {
                return files[0].delete()
            } catch (e: Exception) {
                log.error(ExceptionLogItem("", e))

                throw FileApiException("")
            }
        }

        return false
    }
}
