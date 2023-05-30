package com.hrblizz.fileapi.data.entities

import org.springframework.data.annotation.Id

class Entity {
    @Id
    lateinit var name: String
    lateinit var value: String
}
