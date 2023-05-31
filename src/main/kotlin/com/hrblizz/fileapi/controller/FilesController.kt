package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.controller.exception.BadRequestException
import com.hrblizz.fileapi.controller.exception.NotFoundException
import com.hrblizz.fileapi.controller.model.FileMetaResponse
import com.hrblizz.fileapi.controller.model.GetMetasRequest
import com.hrblizz.fileapi.controller.model.GetMetasResponse
import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileEntityRepository
import com.hrblizz.fileapi.rest.CREATE_TIME
import com.hrblizz.fileapi.rest.FILE_NAME
import com.hrblizz.fileapi.rest.FILE_SIZE
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Instant
import java.util.UUID
import javax.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.Base64Utils.decodeFromString
import org.springframework.util.Base64Utils.encodeToString
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Validated
@RestController
@Tag(name = "Files")
class FilesController(
    private val fileEntityRepository: FileEntityRepository
) {

    @PostMapping("/files")
    @Operation(summary = "Upload file")
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadFiles(
        @RequestParam("name") name: String,
        @RequestParam("contentType") contentType: String,

        @RequestParam("meta")
        @Schema(description = "JSON of additional meta", example = "{\"creatorEmployeeId\": 1}")
        meta: String,

        @RequestParam("source")
        @Schema(example = "timesheet, mss, hrb, ...")
        source: String,

        @RequestParam("expireTime") expireTime: Instant?,

        @RequestParam("content")
        @Schema(description = "File content")
        file: MultipartFile
    ): Map<String, Any> {

        return mapOf(
            "token" to fileEntityRepository.save(
                FileEntity(
                    filename = name,
                    contentType = contentType,
                    meta = meta,
                    source = source,
                    expireTime = expireTime,
                    content = encodeToString(file.bytes),
                    size = file.size
                )
            ).token
        )
    }

    @PostMapping("/files/metas")
    @Operation(summary = "Get files metadata")
    fun getMetas(@RequestBody @Valid request: GetMetasRequest): ResponseEntity<GetMetasResponse> {
        val filesMetas = fileEntityRepository.findByTokenIn(request.tokens)

        return ResponseEntity<GetMetasResponse>(
            GetMetasResponse(
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
            ),
            HttpStatus.OK
        )
    }

    @GetMapping("/file/{token}")
    @Operation(summary = "Download file")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200"),
            ApiResponse(responseCode = "404")
        ]
    )
    fun downloadFile(@PathVariable("token") token: String): ResponseEntity<Resource> {
        val entity = fileEntityRepository.findByToken(token)
            ?: throw NotFoundException("File with token = $token was not found")

        return ResponseEntity.ok()
            .header(FILE_NAME, entity.filename)
            .header(FILE_SIZE, entity.size.toString())
            .header(CREATE_TIME, entity.createTime.toString())
            .header(HttpHeaders.CONTENT_TYPE, entity.contentType)
            .body(ByteArrayResource(decodeFromString(entity.content)))
    }

    @DeleteMapping("/file/{token}")
    @Operation(summary = "Delete file")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFile(@PathVariable("token") token: String) {
        fileEntityRepository.deleteByToken(token)
    }
}
