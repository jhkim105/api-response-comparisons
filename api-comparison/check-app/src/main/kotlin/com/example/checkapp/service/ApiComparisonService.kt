package com.example.checkapp.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class ApiComparisonService(
    private val objectMapper: ObjectMapper
) {
    private val v1Client = WebClient.builder()
        .baseUrl("http://localhost:8081")
        .build()
    
    private val v2Client = WebClient.builder()
        .baseUrl("http://localhost:8082")
        .build()

    fun compareUsers(): Mono<ComparisonResult> {
        val v1Response = v1Client.get()
            .uri("/api/v1/users")
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v1 users")

        val v2Response = v2Client.get()
            .uri("/api/v2/users")
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v2 users")

        return Mono.zip(v1Response, v2Response) { v1, v2 ->
            compareResponses("users", v1, v2)
        }
    }

    fun compareUser(id: Long): Mono<ComparisonResult> {
        val v1Response = v1Client.get()
            .uri("/api/v1/users/{id}", id)
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v1 user $id")

        val v2Response = v2Client.get()
            .uri("/api/v2/users/{id}", id)
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v2 user $id")

        return Mono.zip(v1Response, v2Response) { v1, v2 ->
            compareResponses("user/$id", v1, v2)
        }
    }

    fun compareProducts(): Mono<ComparisonResult> {
        val v1Response = v1Client.get()
            .uri("/api/v1/products")
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v1 products")

        val v2Response = v2Client.get()
            .uri("/api/v2/products")
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v2 products")

        return Mono.zip(v1Response, v2Response) { v1, v2 ->
            compareResponses("products", v1, v2)
        }
    }

    fun compareProduct(id: Long): Mono<ComparisonResult> {
        val v1Response = v1Client.get()
            .uri("/api/v1/products/{id}", id)
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v1 product $id")

        val v2Response = v2Client.get()
            .uri("/api/v2/products/{id}", id)
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v2 product $id")

        return Mono.zip(v1Response, v2Response) { v1, v2 ->
            compareResponses("product/$id", v1, v2)
        }
    }

    fun compareHealth(): Mono<ComparisonResult> {
        val v1Response = v1Client.get()
            .uri("/api/v1/health")
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v1 health")

        val v2Response = v2Client.get()
            .uri("/api/v2/health")
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorReturn("ERROR: Failed to fetch v2 health")

        return Mono.zip(v1Response, v2Response) { v1, v2 ->
            compareResponses("health", v1, v2)
        }
    }

    private fun compareResponses(endpoint: String, v1Response: String, v2Response: String): ComparisonResult {
        return try {
            val v1Json = objectMapper.readTree(v1Response)
            val v2Json = objectMapper.readTree(v2Response)
            
            val differences = findDifferences(v1Json, v2Json)
            val isIdentical = differences.isEmpty()
            
            ComparisonResult(
                endpoint = endpoint,
                isIdentical = isIdentical,
                v1Response = v1Response,
                v2Response = v2Response,
                differences = differences,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            ComparisonResult(
                endpoint = endpoint,
                isIdentical = false,
                v1Response = v1Response,
                v2Response = v2Response,
                differences = listOf("Error parsing JSON: ${e.message}"),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun findDifferences(v1Json: JsonNode, v2Json: JsonNode, path: String = ""): List<String> {
        val differences = mutableListOf<String>()
        
        // Get all field names from both objects
        val v1Fields = v1Json.fieldNames().asSequence().toSet()
        val v2Fields = v2Json.fieldNames().asSequence().toSet()
        
        // Fields only in v1
        (v1Fields - v2Fields).forEach { field ->
            differences.add("Field '$path$field' exists only in v1")
        }
        
        // Fields only in v2
        (v2Fields - v1Fields).forEach { field ->
            differences.add("Field '$path$field' exists only in v2")
        }
        
        // Common fields with different values
        (v1Fields intersect v2Fields).forEach { field ->
            val v1Value = v1Json.get(field)
            val v2Value = v2Json.get(field)
            
            if (v1Value != v2Value) {
                if (v1Value.isObject && v2Value.isObject) {
                    differences.addAll(findDifferences(v1Value, v2Value, "$path$field."))
                } else {
                    differences.add("Field '$path$field' differs: v1='$v1Value', v2='$v2Value'")
                }
            }
        }
        
        return differences
    }
}

data class ComparisonResult(
    val endpoint: String,
    val isIdentical: Boolean,
    val v1Response: String,
    val v2Response: String,
    val differences: List<String>,
    val timestamp: Long
)