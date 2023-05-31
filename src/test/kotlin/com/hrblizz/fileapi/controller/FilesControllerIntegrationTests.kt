package com.hrblizz.fileapi.controller

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.hrblizz.fileapi.Application
import com.hrblizz.fileapi.controller.model.GetMetasRequest
import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileEntityRepository
import com.hrblizz.fileapi.library.JsonUtil.toJson
import com.jayway.jsonpath.JsonPath
import java.time.Instant
import java.util.UUID
import org.hamcrest.Matchers.aMapWithSize
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.Base64Utils

private const val USER = "admin"
private const val PASSWORD = "hunter2"

@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Tag("integration")
@AutoConfigureMockMvc
internal class FilesControllerIntegrationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val repository: FileEntityRepository
) {

    @BeforeEach
    fun beforeEach() {
        repository.deleteAll()
    }

    @Nested
    inner class UploadFile {

        @Test
        fun `when upload file should return uploaded file token`() {
            val mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/files")
                    .file(
                        MockMultipartFile(
                            "content",
                            "test_file.pdf",
                            "application/pdf",
                            "123".toByteArray()
                        )
                    )
                    .param("name", "test_file.pdf")
                    .param("contentType", "application/pdf")
                    .param("meta", "{}")
                    .param("source", "source")
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
            }
        }
    }

    @Nested
    inner class GetFileMetas {

        @Test
        fun `with single valid token returns file meta`() {
            val givenFile = repository.save(newFileEntity())
            val givenToken = givenFile.token
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
                .andExpect(jsonPath("$.files.$givenToken.fileName").value(givenFile.filename))
                .andExpect(jsonPath("$.files.$givenToken.size").value(givenFile.size))
                .andExpect(jsonPath("$.files.$givenToken.contentType").value(givenFile.contentType))
                .andExpect(jsonPath("$.files.$givenToken.createTime").value(givenFile.createTime.toString()))
                .andExpect(jsonPath("$.files.$givenToken.meta").value(givenFile.meta))
        }

        @Test
        fun `with single invalid token returns ok and empty files list`() {
            val givenFile = repository.save(newFileEntity())
            val givenToken = UUID.randomUUID()
            assert(givenFile.token != givenToken.toString())

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
    }

    @Nested
    inner class DownloadFile {

        @Test
        fun `with invalid token should return not found`() {
            val givenFile = repository.save(newFileEntity())
            val givenToken = UUID.randomUUID()
            assert(givenFile.token != givenToken.toString())

            mockMvc.perform(
                MockMvcRequestBuilders.get("/file/$givenToken").with(basicAuth())
            ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$", aMapWithSize<Any, Any>(3)))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors", hasSize<Any>(1)))
                .andExpect(jsonPath("$.errors[0]", aMapWithSize<Any, Any>(2)))
                .andExpect(jsonPath("$.errors[0].message")
                    .value("File with token = $givenToken was not found"))
                .andExpect(jsonPath("$.errors[0].code").value(nullValue()))
                .andExpect(jsonPath("$.status").value(404))
        }
    }


    private fun basicAuth(): RequestPostProcessor =
        SecurityMockMvcRequestPostProcessors.httpBasic(USER, PASSWORD)

    private fun newFileEntity() = FileEntity(
        filename = "testFileName.txt",
        size = 123,
        contentType = "text/plain",
        createTime = Instant.ofEpochMilli(123456789),
        meta = JSONObject(mapOf("creatorEmployeeId" to 1)).toString(),
        source = "testSource",
        content = Base64Utils.encodeToString("I cook with wine, sometimes I even add it to the food.".toByteArray())
    )

    private fun createGetMetasRequest(vararg tokens: String) =
        GetMetasRequest(tokens.asList())
}
