package com.example.checkapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CheckAppApplication

fun main(args: Array<String>) {
    runApplication<CheckAppApplication>(*args)
}