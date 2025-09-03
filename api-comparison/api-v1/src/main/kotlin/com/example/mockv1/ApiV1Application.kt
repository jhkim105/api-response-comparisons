package com.example.mockv1

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiV1Application

fun main(args: Array<String>) {
    runApplication<ApiV1Application>(*args)
}