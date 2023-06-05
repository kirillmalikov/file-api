package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.command.DeleteFileCommand
import com.hrblizz.fileapi.command.DownloadFileCommand
import com.hrblizz.fileapi.command.UploadFileCommand
import com.hrblizz.fileapi.command.GetFilesMetasCommand
import com.hrblizz.fileapi.controller.model.GetMetasRequest
import com.hrblizz.fileapi.controller.model.GetMetasResponse
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
import javax.validation.Valid
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
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
    private val uploadFileCommand: UploadFileCommand,
    private val downloadFileCommand: DownloadFileCommand,
    private val deleteFileCommand: DeleteFileCommand,
    private val getFilesMetasCommand: GetFilesMetasCommand,
) {

    @PostMapping("/files")
    @Operation(summary = "Upload file")
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadFiles(
        @RequestParam("name") name: String,
        @RequestParam("contentType") contentType: String,
        @RequestParam("meta")
        @Schema(description = "JSON of additional meta", example = "{\"creatorEmployeeId\": 1}") meta: String,
        @RequestParam("source")
        @Schema(example = "timesheet, mss, hrb, ...") source: String,
        @RequestParam("expireTime") expireTime: Instant?,
        @RequestParam("content")
        @Schema(description = "File content") file: MultipartFile
    ): Map<String, Any> {

        return mapOf(
            "token" to uploadFileCommand.execute(
                UploadFileCommand.Parameters(
                    filename = name,
                    contentType = contentType,
                    meta = meta,
                    source = source,
                    expireTime = expireTime,
                    size = file.size,
                    content = file.inputStream
                )
            )
        )
    }

    @PostMapping("/files/metas")
    @Operation(summary = "Get files metadata")
    fun getMetas(@RequestBody @Valid request: GetMetasRequest): ResponseEntity<GetMetasResponse> {
        val metasResponse = getFilesMetasCommand.execute(GetFilesMetasCommand.Parameters(request.tokens))

        return ResponseEntity<GetMetasResponse>(
            metasResponse,
            HttpStatus.OK
        )
    }

    @GetMapping("/file/{token}")
    @Operation(summary = "Download file")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200"),
            ApiResponse(responseCode = "404"),
            ApiResponse(responseCode = "503")
        ]
    )
    fun downloadFile(@PathVariable("token") token: String): ResponseEntity<Resource> {
        val file = downloadFileCommand.execute(token)
        val fileDoc = file.first
        val fileContent = file.second

        return ResponseEntity.ok()
            .header(FILE_NAME, fileDoc.filename)
            .header(FILE_SIZE, fileDoc.size.toString())
            .header(CREATE_TIME, fileDoc.createTime.toString())
            .header(HttpHeaders.CONTENT_TYPE, fileDoc.contentType)
            .body(InputStreamResource(fileContent))
    }

    @DeleteMapping("/file/{token}")
    @Operation(summary = "Delete file")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteFile(@PathVariable("token") token: String) {
        deleteFileCommand.execute(token)
    }
}
