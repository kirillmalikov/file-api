package com.hrblizz.fileapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION


@Configuration
internal class SwaggerConfig {

    @Bean
    fun publicUserApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("Files")
            .pathsToMatch("/files/*", "/file/*")
            .build()
    }

    @Bean
    fun customOpenApi(): OpenAPI {
        return OpenAPI().info(
            Info()
                .title("Welcome to the File API")
                .contact(Contact().email("kirill.malikov@mail.com"))
                .description("Test assignment: File API")
                .version("1.0")
        ).servers(listOf(Server().url("http://localhost:6011/")))
            .components(
                Components()
                    .addSecuritySchemes(
                        "Basic", SecurityScheme()
                            .name(AUTHORIZATION)
                            .type(SecurityScheme.Type.HTTP)
                            .description(
                                """
                                    File API uses basic HTTP authentication,
                                    a request contains a header field in the
                                    form of <b>Authorization: Basic \<credentials\></b>,
                                    where credentials is the Base64 encoding of
                                    user and password joined by a single colon <b>:</b>
                                """.trimIndent()
                            )
                    )
            )
            .addSecurityItem(
                SecurityRequirement().addList("Basic", emptyList())
            )
    }
}
