package com.hrblizz.fileapi.controller

import com.hrblizz.fileapi.data.entities.Entity
import com.hrblizz.fileapi.data.repository.EntityRepository
import com.hrblizz.fileapi.rest.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StatusController(
    private val entityRepository: EntityRepository
) {
    @RequestMapping("/status", method = [RequestMethod.GET])
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        entityRepository.save(
            Entity().also {
                it.name = UUID.randomUUID().toString()
                it.value = "asd"
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
