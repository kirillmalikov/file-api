package com.hrblizz.fileapi.security

import com.hrblizz.fileapi.library.LoggerRequestInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val loggerRequestInterceptor: LoggerRequestInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggerRequestInterceptor)
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/docs").setViewName("forward:/docs/index.html")
    }
}
