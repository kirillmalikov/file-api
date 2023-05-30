package com.hrblizz.fileapi.library.log

open class TraceLogItem constructor(
    method: String,
    url: String,
    status: Long,
    duration: Long
) : LogItem("$method $url => $status in $duration ms") {
    init {
        this.type = "trace"
    }
}
