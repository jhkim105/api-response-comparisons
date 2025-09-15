package com.example.shadowing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiShadowingCoroutineApplication

fun main(args: Array<String>) {
    runApplication<ApiShadowingCoroutineApplication>(*args)
}
