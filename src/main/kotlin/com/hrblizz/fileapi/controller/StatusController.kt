package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.data.entities.Entity
import com.hrblizz.fileapi.data.repository.EntityRepository
import com.hrblizz.fileapi.rest.ResponseEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Status")
class StatusController(
    private val entityRepository: EntityRepository
) {

    @GetMapping("/status")
    @Operation(summary = "Get status")
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        entityRepository.save(
            Entity().apply {
                name = UUID.randomUUID().toString()
                value = "asd"
            }
        )

        return ResponseEntity(
            mapOf(
                "ok" to true
            ),
            HttpStatus.OK.value()
        )
    }
}
