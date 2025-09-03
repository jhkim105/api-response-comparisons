package com.example.compare

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient

@RestController
class CompareController(
    private val mapper: ObjectMapper,
    @Value("\${endpoints.v1:http://localhost:8081/api/v1}") private val v1Base: String,
    @Value("\${endpoints.v2:http://localhost:8082/api/v2}") private val v2Base: String
) {
    private val client = WebClient.builder().build()

    data class ValueMismatch(val path: String, val v1: JsonNode?, val v2: JsonNode?)
    data class DiffResult(
        val equal: Boolean,
        val missingInV2: List<String>,
        val missingInV1: List<String>,
        val valueMismatches: List<ValueMismatch>
    )

    data class CompareResponse(val id: String, val v1Url: String, val v2Url: String, val diff: DiffResult)

    @GetMapping("/compare/users/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun compareUser(@PathVariable id: Long): CompareResponse {
        val v1Url = "$v1Base/users/$id"
        val v2Url = "$v2Base/users/$id"
        val v1 = fetch(v1Url)
        val v2 = fetch(v2Url)
        val diff = diffJson("", v1, v2)
        return CompareResponse(id.toString(), v1Url, v2Url, diff)
    }

    private suspend fun fetch(url: String): JsonNode =
        client.get().uri(url).retrieve().bodyToMono(String::class.java).map { mapper.readTree(it) }.awaitSingle()

    private fun diffJson(prefix: String, a: JsonNode?, b: JsonNode?): DiffResult {
        val missingInV2 = mutableListOf<String>()
        val missingInV1 = mutableListOf<String>()
        val valueMismatches = mutableListOf<ValueMismatch>()

        fun walk(path: String, left: JsonNode?, right: JsonNode?) {
            when {
                left == null && right != null -> missingInV1 += path
                left != null && right == null -> missingInV2 += path
                left == null && right == null -> {}
                left!!.isValueNode && right!!.isValueNode -> if (left != right) valueMismatches += ValueMismatch(
                    path,
                    left,
                    right
                )

                left.isObject && right!!.isObject -> {
                    val fieldNames = (left.fieldNames().asSequence().toSet() + right.fieldNames().asSequence().toSet())
                    for (name in fieldNames) walk("$path/$name", left.get(name), right.get(name))
                }

                left.isArray && right!!.isArray -> {
                    val max = maxOf(left.size(), right.size())
                    for (i in 0 until max) walk("$path[$i]", left.get(i), right.get(i))
                }

                else -> valueMismatches += ValueMismatch(path, left, right)
            }
        }

        walk(prefix, a, b)
        val equal = missingInV1.isEmpty() && missingInV2.isEmpty() && valueMismatches.isEmpty()
        return DiffResult(equal, missingInV2, missingInV1, valueMismatches)
    }
}
