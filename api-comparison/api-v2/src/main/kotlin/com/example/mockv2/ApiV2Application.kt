package com.example.mockv2

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiV2Application

fun main(args: Array<String>) {
    runApplication<ApiV2Application>(*args)
}