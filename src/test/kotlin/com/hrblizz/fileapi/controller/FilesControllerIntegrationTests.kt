package com.hrblizz.fileapi.controller

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.hrblizz.fileapi.Application
import com.hrblizz.fileapi.controller.model.GetMetasRequest
import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileEntityRepository
import com.hrblizz.fileapi.library.JsonUtil.toJson
import com.hrblizz.fileapi.rest.CREATE_TIME
import com.hrblizz.fileapi.rest.FILE_NAME
import com.hrblizz.fileapi.rest.FILE_SIZE
import com.hrblizz.fileapi.service.LocalFileStorage
import com.jayway.jsonpath.JsonPath
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.random.Random
import org.apache.commons.io.FileUtils
import org.hamcrest.Matchers.aMapWithSize
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

private const val USER = "admin"
private const val PASSWORD = "hunter2"

private const val MOCK_FILE_PATH = "src/test/resources/mock/file/test.txt"

@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Tag("integration")
@AutoConfigureMockMvc
internal class FilesControllerIntegrationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val repository: FileEntityRepository,
    @Autowired private val localFileStorage: LocalFileStorage,
    @Value("\${localstorage.directory}") private val directory: String
) {

    @BeforeEach
    fun beforeEach() {
        setUpStorage()
    }

    @AfterEach
    fun afterEach() {
        cleanUpStorage()
    }

    @Nested
    inner class UploadFile {

        @Test
        fun `when upload file should return uploaded file token`() {
            val fileInputStream = File(MOCK_FILE_PATH).inputStream()
            val givenFile = MockMultipartFile("content", "test.txt", "text/plain", fileInputStream)

            val mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/files")
                    .file(givenFile)
                    .param("name", "test.txt")
                    .param("contentType", "text/plain")
                    .param("meta", "{}")
                    .param("source", this::class.java.simpleName)
                    .with(basicAuth())
            ).andExpect(status().isCreated)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(1)))
                .andExpect(jsonPath("$.token").isNotEmpty)
                .andReturn()

            val token = JsonPath.read(mvcResult.response.contentAsString, "$.token") as String
            val docs = repository.findAll()

            assertAll {
                assertThat(docs.size).isEqualTo(1)
                assertThat(docs[0].token).isEqualTo(token)
                assertThat(docs[0].filename).isEqualTo(givenFile.originalFilename)
                assertThat(docs[0].size).isEqualTo(givenFile.size)
                assertThat(docs[0].contentType).isEqualTo(givenFile.contentType)
                assertThat(docs[0].createTime).isNotNull()
                assertThat(docs[0].expireTime).isNull()
                assertThat(docs[0].meta).isEqualTo("{}")
                assertThat(docs[0].source).isEqualTo("UploadFile")
            }
        }

        @Test
        fun `with missing parameter should return bad request`() {
            mockMvc.perform(
                MockMvcRequestBuilders.multipart("/files")
                    .file(
                        MockMultipartFile(
                            "content",
                            "test_file.pdf",
                            "application/pdf",
                            "123".toByteArray()
                        )
                    )
                    .param("contentType", "application/pdf")
                    .param("meta", "{}")
                    .param("source", "source")
                    .with(basicAuth())
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(jsonPath("$.errors[0].message").value("name parameter is missing"))
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(400))
                .andReturn()
        }

        @Test
        fun `without auth should return unauthorized`() {
            mockMvc.perform(
                MockMvcRequestBuilders.multipart("/files")
            ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(
                    jsonPath("$.errors[0].message")
                        .value("Full authentication is required to access this resource")
                )
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(401))
        }
    }

    @Nested
    inner class GetFileMetas {

        @Test
        fun `with single valid token returns file meta`() {
            val givenFileDoc = repository.save(newFileEntity())
            val givenToken = givenFileDoc.token
            val request = toJson(createGetMetasRequest(givenToken), formatDates = true)

            mockMvc.perform(
                MockMvcRequestBuilders.post("/files/metas")
                    .contentType("application/json")
                    .content(request!!)
                    .with(basicAuth())
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(1)))
                .andExpect(jsonPath("$.files", aMapWithSize<Any, Any>(1)))
                .andExpect(jsonPath("$.files", hasKey(givenToken)))
                .andExpect(jsonPath("$.files.$givenToken", aMapWithSize<Any, Any>(6)))
                .andExpect(jsonPath("$.files.$givenToken.token").value(givenToken))
                .andExpect(jsonPath("$.files.$givenToken.fileName").value(givenFileDoc.filename))
                .andExpect(jsonPath("$.files.$givenToken.size").value(givenFileDoc.size))
                .andExpect(jsonPath("$.files.$givenToken.contentType").value(givenFileDoc.contentType))
                .andExpect(jsonPath("$.files.$givenToken.createTime").value(givenFileDoc.createTime.toString()))
                .andExpect(jsonPath("$.files.$givenToken.meta").value(givenFileDoc.meta))
        }

        @Test
        fun `with multiple valid tokens returns files metas`() {
            val givenFileDoc1 = repository.save(newFileEntity())
            val givenFileDoc2 = repository.save(newFileEntity())
            val givenToken1 = givenFileDoc1.token
            val givenToken2 = givenFileDoc2.token
            val request = toJson(createGetMetasRequest(givenToken1, givenToken2))

            mockMvc.perform(
                MockMvcRequestBuilders.post("/files/metas")
                    .contentType("application/json")
                    .content(request!!)
                    .with(basicAuth())
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(1)))
                .andExpect(jsonPath("$.files", aMapWithSize<Any, Any>(2)))
                .andExpect(jsonPath("$.files", hasKey(givenToken1)))
                .andExpect(jsonPath("$.files", hasKey(givenToken2)))
                .andExpect(jsonPath("$.files.$givenToken1", aMapWithSize<Any, Any>(6)))
                .andExpect(jsonPath("$.files.$givenToken1.token").value(givenToken1))
                .andExpect(jsonPath("$.files.$givenToken1.fileName").value(givenFileDoc1.filename))
                .andExpect(jsonPath("$.files.$givenToken1.size").value(givenFileDoc1.size))
                .andExpect(jsonPath("$.files.$givenToken1.contentType").value(givenFileDoc1.contentType))
                .andExpect(jsonPath("$.files.$givenToken1.createTime").value(givenFileDoc1.createTime.toString()))
                .andExpect(jsonPath("$.files.$givenToken1.meta").value(givenFileDoc1.meta))
                .andExpect(jsonPath("$.files.$givenToken2", aMapWithSize<Any, Any>(6)))
                .andExpect(jsonPath("$.files.$givenToken2.token").value(givenToken2))
                .andExpect(jsonPath("$.files.$givenToken2.fileName").value(givenFileDoc2.filename))
                .andExpect(jsonPath("$.files.$givenToken2.size").value(givenFileDoc2.size))
                .andExpect(jsonPath("$.files.$givenToken2.contentType").value(givenFileDoc2.contentType))
                .andExpect(jsonPath("$.files.$givenToken2.createTime").value(givenFileDoc2.createTime.toString()))
                .andExpect(jsonPath("$.files.$givenToken2.meta").value(givenFileDoc2.meta))
        }

        @Test
        fun `with single invalid token returns ok and empty files list`() {
            val givenFileDoc = repository.save(newFileEntity())
            val givenToken = UUID.randomUUID()
            assertThat(givenFileDoc.token).isNotEqualTo(givenToken.toString())

            val request = toJson(createGetMetasRequest(givenToken.toString()), formatDates = true)

            mockMvc.perform(
                MockMvcRequestBuilders.post("/files/metas")
                    .contentType("application/json")
                    .content(request!!)
                    .with(basicAuth())
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(1)))
                .andExpect(jsonPath("$.files", aMapWithSize<Any, Any>(0)))
        }

        @Test
        fun `with null request body should return bad request`() {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/files/metas")
                    .contentType("application/json")
                    .with(basicAuth())
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(jsonPath("$.errors[0].message").value("Bad Request"))
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(400))
        }

        @Test
        fun `with null tokens list in request body should return bad request`() {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/files/metas")
                    .contentType("application/json")
                    .content("{}")
                    .with(basicAuth())
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(jsonPath("$.errors[0].message").value("Bad Request"))
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(400))
        }

        @Test
        fun `with empty tokens list in request body should return bad request`() {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/files/metas")
                    .contentType("application/json")
                    .content(toJson(GetMetasRequest(listOf()))!!)
                    .with(basicAuth())
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(jsonPath("$.errors[0].message").value("tokens: must not be empty"))
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(400))
        }

        @Test
        fun `without auth should return unauthorized`() {
            mockMvc.perform(
                MockMvcRequestBuilders.post("/files/metas")
                    .contentType("application/json")
            ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(
                    jsonPath("$.errors[0].message")
                        .value("Full authentication is required to access this resource")
                )
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(401))
        }
    }

    @Nested
    inner class DownloadFile {

        @Test
        fun `with invalid token should return not found`() {
            val givenFileDoc = repository.save(newFileEntity())
            val givenToken = UUID.randomUUID()
            assertThat(givenFileDoc.token).isNotEqualTo(givenToken.toString())

            mockMvc.perform(
                MockMvcRequestBuilders.get("/file/$givenToken").with(basicAuth())
            ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(
                    jsonPath("$.errors[0].message")
                        .value("File document with token = $givenToken was not found")
                )
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(404))
        }

        @Test
        fun `with file absent in storage should return not found`() {
            val givenFileDoc = repository.save(newFileEntity())
            val givenToken = givenFileDoc.token

            mockMvc.perform(
                MockMvcRequestBuilders.get("/file/$givenToken").with(basicAuth())
            ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(
                    jsonPath("$.errors[0].message")
                        .value("File was not found in storage")
                )
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(404))
        }

        @Test
        fun `with valid token should download the file and return correct headers`() {
            val givenToken = UUID.randomUUID().toString()
            val givenFile = File(MOCK_FILE_PATH)
            val givenFileDoc = repository.save(
                FileEntity(
                    token = givenToken,
                    filename = givenFile.name,
                    size = 123,
                    contentType = "text/plain",
                    meta = "{}",
                    source = "test"
                )
            )

            val fileKey = "$givenToken.${givenFile.name}"

            localFileStorage.storeFile(givenFile.inputStream(), fileKey)

            val mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/file/$givenToken").with(basicAuth())
            ).andExpect(status().isOk)
                .andReturn()

            val response = mvcResult.response
            val resultFile = kotlin.io.path.createTempFile().toFile()
            resultFile.writeBytes(response.contentAsByteArray)
            val headers = response.headerNames

            assertThat(FileUtils.contentEquals(resultFile, givenFile)).isTrue()
            assertThat(headers.contains(FILE_NAME)).isTrue()
            assertThat(headers.contains(FILE_SIZE)).isTrue()
            assertThat(headers.contains(CREATE_TIME)).isTrue()
            assertThat(response.getHeader(FILE_NAME)).isEqualTo(givenFileDoc.filename)
            assertThat(response.getHeader(FILE_SIZE)).isEqualTo(givenFileDoc.size.toString())
            assertThat(response.getHeader(CREATE_TIME)).isEqualTo(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .format(Date.from(givenFileDoc.createTime))
            )
        }

        @Test
        fun `without auth should return unauthorized`() {
            mockMvc.perform(
                MockMvcRequestBuilders.get("/file/token")
            ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(
                    jsonPath("$.errors[0].message")
                        .value("Full authentication is required to access this resource")
                )
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(401))
        }
    }

    @Nested
    inner class DeleteFile {

        @Test
        fun `with valid token should delete file and file doc and return no content`() {
            val givenToken = UUID.randomUUID().toString()
            val givenFile = File(MOCK_FILE_PATH)
            repository.save(
                FileEntity(
                    token = givenToken,
                    filename = givenFile.name,
                    size = 123,
                    contentType = "text/plain",
                    meta = "{}",
                    source = "test"
                )
            )

            val fileKey = "$givenToken.${givenFile.name}"

            localFileStorage.storeFile(givenFile.inputStream(), fileKey)

            assertAll {
                assertThat(File(directory + fileKey).exists()).isTrue()
                assertThat(repository.existsByToken(givenToken)).isTrue()
            }

            mockMvc.perform(
                MockMvcRequestBuilders.delete("/file/$givenToken").with(basicAuth())
            ).andExpect(status().isNoContent)
                .andReturn()

            assertAll {
                assertThat(File(directory + fileKey).exists()).isFalse()
                assertThat(repository.existsByToken(givenToken)).isFalse()
            }
        }

        @Test
        fun `with invalid token should return not found`() {
            val invalidToken = UUID.randomUUID().toString()
            val givenFile = File(MOCK_FILE_PATH)
            val givenFileDoc = repository.save(
                FileEntity(
                    token = UUID.randomUUID().toString(),
                    filename = givenFile.name,
                    size = 123,
                    contentType = "text/plain",
                    meta = "{}",
                    source = "test"
                )
            )
            assertThat(invalidToken).isNotEqualTo(givenFileDoc.token)

            val fileKey = "${givenFileDoc.token}.${givenFile.name}"

            localFileStorage.storeFile(givenFile.inputStream(), fileKey)

            mockMvc.perform(
                MockMvcRequestBuilders.delete("/file/$invalidToken").with(basicAuth())
            ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(
                    jsonPath("$.errors[0].message")
                        .value("File was not found in storage")
                )
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(404))

            assertAll {
                assertThat(File(directory + fileKey).exists()).isTrue()
                assertThat(repository.existsByToken(givenFileDoc.token)).isTrue()
            }
        }

        @Test
        fun `with file absent in storage should return not found`() {
            val givenFileDoc = repository.save(newFileEntity())
            val givenToken = givenFileDoc.token

            mockMvc.perform(
                MockMvcRequestBuilders.delete("/file/$givenToken").with(basicAuth())
            ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(
                    jsonPath("$.errors[0].message")
                        .value("File was not found in storage")
                )
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(404))
        }

        @Test
        fun `without auth should return unauthorized`() {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/file/token")
            ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(
                    jsonPath("$.errors[0].message")
                        .value("Full authentication is required to access this resource")
                )
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(401))
        }
    }

    private fun basicAuth(): RequestPostProcessor =
        SecurityMockMvcRequestPostProcessors.httpBasic(USER, PASSWORD)

    private fun newFileEntity() = FileEntity(
        filename = "testFileName-${Random.nextInt()}.txt",
        size = Random.nextLong(),
        contentType = "text/plain",
        createTime = Instant.ofEpochMilli(123456789),
        meta = JSONObject(mapOf("creatorEmployeeId" to 1)).toString(),
        source = "testSource"
    )

    private fun createGetMetasRequest(vararg tokens: String) =
        GetMetasRequest(tokens.asList())

    private fun setUpStorage() {
        File(directory).mkdirs()
        repository.deleteAll()
        FileUtils.cleanDirectory(File(directory))
    }

    private fun cleanUpStorage() {
        repository.deleteAll()
        FileUtils.cleanDirectory(File(directory))
    }
}
