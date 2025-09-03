package com.example.v1

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class UserV1Controller {
    data class UserV1(val id: Long, val name: String, val email: String)

    @GetMapping("/api/v1/users/{id}")
    fun getUser(@PathVariable id: Long) = UserV1(
        id, if (id % 2L == 0L) "Alice Kim" else "Bob Lee",
        if (id % 2L == 0L) "alice@example.com" else "bob@example.com"
    )
}
