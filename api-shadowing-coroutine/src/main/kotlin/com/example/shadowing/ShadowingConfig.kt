package com.example.shadowing

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClient
import java.util.concurrent.Executors

@Configuration
class ShadowingConfig {

    /** 전용 워커 디스패처 (비동기 검증용) */
    @Bean(name = ["shadowingDispatcher"], destroyMethod = "close")
    fun shadowingDispatcher(): ExecutorCoroutineDispatcher {
        val threads = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
        val executor = Executors.newFixedThreadPool(threads) { r ->
            Thread(r, "shadowing-$threads").apply { isDaemon = true }
        }
        return executor.asCoroutineDispatcher() // Closeable → destroyMethod="close"
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