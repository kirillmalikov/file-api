package com.hrblizz.fileapi.service

import java.io.File
import java.io.InputStream

interface FileStorage {
    fun storeFile(inputStream: InputStream, key: String): Boolean
    fun getFile(key: String): File
    fun deleteFile(key: String): Boolean
}
