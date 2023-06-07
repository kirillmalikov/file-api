package com.hrblizz.fileapi.command

import com.hrblizz.fileapi.controller.model.FileMetaResponse
import com.hrblizz.fileapi.controller.model.GetMetasResponse
import com.hrblizz.fileapi.data.repository.FileEntityRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GetFilesMetasCommand(
    private val fileEntityRepository: FileEntityRepository
) {

    fun execute(parameters: Parameters): GetMetasResponse {
        val filesMetas = fileEntityRepository.findByTokenIn(parameters.tokens)

        return GetMetasResponse(
            filesMetas.associateBy(
                { it.token },
                {
                    FileMetaResponse(
                        UUID.fromString(it.token),
                        it.filename,
                        it.size,
                        it.contentType,
                        it.createTime,
                        it.meta
                    )
                }
            )
        )
    }

    data class Parameters(
        val tokens: Collection<String>
    )
}
