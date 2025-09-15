package com.example.shadowing

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestClient

@Configuration
class ShadowingConfig {

    @Bean("shadowingExecutor")
    fun shadowingExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 16
            queueCapacity = 1000
            threadNamePrefix = "shadowing-"
            setWaitForTasksToCompleteOnShutdown(false)
            initialize()
        }

    @Bean
    fun shadowingRestClient(objectMapper: ObjectMapper): RestClient =
        RestClient.builder()
            .messageConverters {
                it.removeIf { c -> c is MappingJackson2HttpMessageConverter }
                it.add(MappingJackson2HttpMessageConverter(objectMapper))
            }
            .build()
}