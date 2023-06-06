package com.hrblizz.fileapi.data.repository

import com.hrblizz.fileapi.data.entities.FileEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface FileEntityRepository : MongoRepository<FileEntity, Long> {

    fun findByToken(token: String): FileEntity?

    fun findByTokenIn(tokens: Collection<String>): List<FileEntity>

    fun deleteByToken(token: String): Long

    fun existsByToken(token: String): Boolean
}
