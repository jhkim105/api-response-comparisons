package com.example.shadowing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiShadowingApplication

fun main(args: Array<String>) {
    runApplication<ApiShadowingApplication>(*args)
}
