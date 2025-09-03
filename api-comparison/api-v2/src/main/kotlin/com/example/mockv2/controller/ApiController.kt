package com.example.mockv2.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2")
class ApiController {

    @GetMapping("/users/{id}")
    fun getUser(@PathVariable id: Long): UserV2 {
        return UserV2(
            id = id,
            name = "User $id",
            email = "user$id@example.com",
            age = 25 + (id % 50).toInt()
        )
    }

    @GetMapping("/users")
    fun getUsers(): List<UserV2> {
        return listOf(
            UserV2(1, "John Doe", "john@example.com", 30),
            UserV2(2, "Jane Smith", "jane@example.com", 25),
            UserV2(3, "Bob Johnson", "bob@example.com", 35)
        )
    }

    @GetMapping("/products/{id}")
    fun getProduct(@PathVariable id: Long): ProductV2 {
        return ProductV2(
            id = id,
            name = "Product $id",
            price = 100.0 + (id * 10),
            category = "Category ${id % 3 + 1}"
        )
    }

    @GetMapping("/products")
    fun getProducts(): List<ProductV2> {
        return listOf(
            ProductV2(1, "Laptop", 999.99, "Electronics"),
            ProductV2(2, "Book", 29.99, "Education"),
            ProductV2(3, "Coffee Mug", 15.99, "Kitchen")
        )
    }

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "version" to "v2",
            "timestamp" to System.currentTimeMillis().toString()
        )
    }
}

data class UserV2(
    val id: Long,
    val name: String,
    val email: String,
    val age: Int
)

data class ProductV2(
    val id: Long,
    val name: String,
    val price: Double,
    val category: String
)