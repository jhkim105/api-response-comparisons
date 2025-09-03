package com.example.mockv1.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class ApiController {

    @GetMapping("/users/{id}")
    fun getUser(@PathVariable id: Long): UserV1 {
        return UserV1(
            id = id,
            name = "User $id",
            email = "user$id@example.com",
            age = 25 + (id % 50).toInt()
        )
    }

    @GetMapping("/users")
    fun getUsers(): List<UserV1> {
        return listOf(
            UserV1(1, "John Doe", "john@example.com", 30),
            UserV1(2, "Jane Smith", "jane@example.com", 25),
            UserV1(3, "Bob Johnson", "bob@example.com", 35)
        )
    }

    @GetMapping("/products/{id}")
    fun getProduct(@PathVariable id: Long): ProductV1 {
        return ProductV1(
            id = id,
            name = "Product $id",
            price = 100.0 + (id * 10),
            category = "Category ${id % 3 + 1}"
        )
    }

    @GetMapping("/products")
    fun getProducts(): List<ProductV1> {
        return listOf(
            ProductV1(1, "Laptop", 999.99, "Electronics"),
            ProductV1(2, "Book", 29.99, "Education"),
            ProductV1(3, "Coffee Mug", 15.99, "Kitchen")
        )
    }

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "version" to "v1",
            "timestamp" to System.currentTimeMillis().toString()
        )
    }
}

data class UserV1(
    val id: Long,
    val name: String,
    val email: String,
    val age: Int
)

data class ProductV1(
    val id: Long,
    val name: String,
    val price: Double,
    val category: String
)