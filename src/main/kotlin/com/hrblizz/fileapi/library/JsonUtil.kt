package com.hrblizz.fileapi.library

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.text.SimpleDateFormat

object JsonUtil {
    /**
     * Safely writes the input object into a JSON string
     */
    fun toJson(obj: Any, usePrettyWriter: Boolean = false, formatDates: Boolean = false): String? {
        try {
            val mapper = ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

            if (formatDates) {
                mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm a z")
            }

            var writer = mapper.writer()

            if (usePrettyWriter) {
                writer = writer.withDefaultPrettyPrinter()
            }
            return writer.writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            // Do nothing
        }

        return null
    }
}
