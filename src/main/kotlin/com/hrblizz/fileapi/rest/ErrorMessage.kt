package com.hrblizz.fileapi.rest

class ErrorMessage {
    var message: String? = null
    var code: String? = null

    constructor() {}

    constructor(message: String?) {
        this.message = message
    }

    constructor(message: String, code: String) {
        this.message = message
        this.code = code
    }
}
