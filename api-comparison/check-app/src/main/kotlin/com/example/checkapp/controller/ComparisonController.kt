package com.example.checkapp.controller

import com.example.checkapp.service.ApiComparisonService
import com.example.checkapp.service.ComparisonResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/compare")
class ComparisonController(
    private val apiComparisonService: ApiComparisonService
) {

    @GetMapping("/users")
    fun compareUsers(): Mono<ComparisonResult> {
        return apiComparisonService.compareUsers()
    }

    @GetMapping("/users/{id}")
    fun compareUser(@PathVariable id: Long): Mono<ComparisonResult> {
        return apiComparisonService.compareUser(id)
    }

    @GetMapping("/products")
    fun compareProducts(): Mono<ComparisonResult> {
        return apiComparisonService.compareProducts()
    }

    @GetMapping("/products/{id}")
    fun compareProduct(@PathVariable id: Long): Mono<ComparisonResult> {
        return apiComparisonService.compareProduct(id)
    }

    @GetMapping("/health")
    fun compareHealth(): Mono<ComparisonResult> {
        return apiComparisonService.compareHealth()
    }

    @GetMapping("/all")
    fun compareAll(): Mono<Map<String, ComparisonResult>> {
        val usersComparison = apiComparisonService.compareUsers()
        val productsComparison = apiComparisonService.compareProducts()
        val healthComparison = apiComparisonService.compareHealth()

        return usersComparison
            .zipWith(productsComparison)
            .zipWith(healthComparison)
            .map { tuple ->
                val users = tuple.t1.t1
                val products = tuple.t1.t2
                val health = tuple.t2
                mapOf(
                    "users" to users,
                    "products" to products,
                    "health" to health
                )
            }
    }

    @GetMapping("/summary")
    fun getSummary(): Mono<ComparisonSummary> {
        return compareAll().map { results ->
            val totalComparisons = results.size
            val identicalCount = results.values.count { it.isIdentical }
            val differentCount = totalComparisons - identicalCount
            val totalDifferences = results.values.sumOf { it.differences.size }

            ComparisonSummary(
                totalComparisons = totalComparisons,
                identicalCount = identicalCount,
                differentCount = differentCount,
                totalDifferences = totalDifferences,
                comparisonDetails = results,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    @GetMapping("/status")
    fun getStatus(): Map<String, Any> {
        return mapOf(
            "service" to "API Comparison Service",
            "status" to "UP",
            "v1ApiUrl" to "http://localhost:8081",
            "v2ApiUrl" to "http://localhost:8082",
            "availableEndpoints" to listOf(
                "/compare/users",
                "/compare/users/{id}",
                "/compare/products",
                "/compare/products/{id}",
                "/compare/health",
                "/compare/all",
                "/compare/summary",
                "/compare/status"
            ),
            "timestamp" to System.currentTimeMillis()
        )
    }
}

data class ComparisonSummary(
    val totalComparisons: Int,
    val identicalCount: Int,
    val differentCount: Int,
    val totalDifferences: Int,
    val comparisonDetails: Map<String, ComparisonResult>,
    val timestamp: Long
)