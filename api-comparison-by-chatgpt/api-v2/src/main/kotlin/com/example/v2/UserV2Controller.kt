package com.example.v2

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class UserV2Controller {
    data class FullName(val first: String, val last: String)
    data class Contacts(val primaryEmail: String)
    enum class Status { ACTIVE, INACTIVE }
    data class UserV2(val userId: String, val fullName: FullName, val contacts: Contacts, val status: Status)

    @GetMapping("/api/v2/users/{id}")
    fun getUser(@PathVariable id: Long) = UserV2(
        id.toString(),
        if (id % 2L == 0L) FullName("Alice", "Kim") else FullName("Bob", "Lee"),
        if (id % 2L == 0L) Contacts("alice@example.com") else Contacts("bob@example.com"),
        if (id % 3L == 0L) Status.INACTIVE else Status.ACTIVE
    )
}
