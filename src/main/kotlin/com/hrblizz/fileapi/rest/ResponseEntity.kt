package com.hrblizz.fileapi.rest

class ResponseEntity<T> {
    var data: T? = null

    var errors: List<ErrorMessage>? = null

    var status: Int = 0
        private set

    constructor(status: Int) {
        this.status = status
    }

    constructor(data: T?, status: Int) : this(data, null, status) {}

    constructor(data: T?, errors: List<ErrorMessage>?, status: Int) {
        this.data = data
        this.errors = errors
        this.status = status
    }
}
