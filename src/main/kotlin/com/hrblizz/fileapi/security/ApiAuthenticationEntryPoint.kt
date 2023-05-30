package com.hrblizz.fileapi.security

import com.hrblizz.fileapi.library.JsonUtil
import com.hrblizz.fileapi.rest.ErrorMessage
import com.hrblizz.fileapi.rest.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
internal class ApiAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authEx: AuthenticationException
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val writer = response.writer
        val responseEntity = ResponseEntity(null, listOf(ErrorMessage(authEx.message)), HttpStatus.UNAUTHORIZED.value())

        writer.println(JsonUtil.toJson(responseEntity))
        writer.flush()
        writer.close()
    }
}
