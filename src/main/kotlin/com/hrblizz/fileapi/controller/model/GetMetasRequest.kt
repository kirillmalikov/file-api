package com.hrblizz.fileapi.controller.model

import javax.validation.constraints.NotEmpty

data class GetMetasRequest(
    @field:NotEmpty
    val tokens: Collection<String>
)
