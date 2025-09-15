package com.example.shadowing

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.diff.JsonDiff
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture.supplyAsync

@Component
class ShadowVerifier(
    private val objectMapper: ObjectMapper,
    private val restClient: RestClient,
    @Qualifier("shadowingExecutor") private val executor: ThreadPoolTaskExecutor,
    @Value("\${shadowing.timeout-ms:1500}") private val timeoutMs: Long
) {
    private val log = KotlinLogging.logger {}

    data class Job(
        val label: String,
        val method: HttpMethod,
        val targetUrl: String,
        val headers: Map<String, String>,
        val hasBody: Boolean,
        val reqBody: Any?,
        val sourceResult: Any,
        val returnType: Type
    )

    fun submit(job: Job) {
        executor.execute {
            try {
                val typeRef = object : ParameterizedTypeReference<Any>() {
                    override fun getType(): Type = job.returnType
                }
                Thread.sleep(2000) // for async test
                log.info { "[API-COMPARE] target api call start." }
                val targetRes = supplyAsync({
                    var spec = restClient.method(job.method).uri(job.targetUrl)
                    job.headers.forEach { (k, v) -> spec = spec.header(k, v) }
                    if (job.hasBody) {
                        spec.contentType(MediaType.APPLICATION_JSON)
                            .body(job.reqBody!!)
                            .retrieve().body(typeRef)!!
                    } else {
                        spec.retrieve().body(typeRef)!!
                    }
                }, executor).get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                log.info { "[API-COMPARE] target api call end." }

                val n1 = objectMapper.readTree(objectMapper.writeValueAsBytes(job.sourceResult))
                val n2 = objectMapper.readTree(objectMapper.writeValueAsBytes(targetRes))

                if (n1 == n2) {
                    log.info { "[API-COMPARE][${job.label}] equal=true" }
                } else {
                    val diff = JsonDiff.asJson(n1, n2)
                    log.warn { "[API-COMPARE][${job.label}] equal=false, diff=$diff" }
                }
            } catch (te: java.util.concurrent.TimeoutException) {
                log.warn(te) { "[API-COMPARE][${job.label}] skipped: target timeout (${timeoutMs}ms)" }
            } catch (e: Throwable) {
                log.warn(e) { "[API-COMPARE][${job.label}] compare failed: ${e.message}" }
            }
        }
    }
}
