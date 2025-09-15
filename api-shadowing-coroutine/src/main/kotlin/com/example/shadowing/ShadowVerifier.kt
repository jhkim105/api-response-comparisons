package com.example.shadowing

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.diff.JsonDiff
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    @Qualifier("shadowingDispatcher") private val dispatcher: CoroutineDispatcher,
    @Value("\${shadowing.timeout-ms:1500}") private val timeoutMs: Long
) {
    private val log = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

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

    /** 어드바이스에서 호출: 코루틴으로 비동기 실행, 즉시 return */
    fun submit(job: Job) {
        scope.launch {
            try {
                withTimeout(timeoutMs) {
                    val typeRef = object : ParameterizedTypeReference<Any>() {
                        override fun getType(): Type = job.returnType
                    }

                    // RestClient는 블로킹 → 전용 디스패처에서 실행
                    delay(1000) // for async test
                    log.info { "[API-COMPARE] target api call start." }
                    val targetRes: Any = withContext(dispatcher) {
                        var spec = restClient.method(job.method).uri(job.targetUrl)
                        job.headers.forEach { (k, v) -> spec = spec.header(k, v) }
                        if (job.hasBody) {
                            spec.contentType(MediaType.APPLICATION_JSON)
                                .body(job.reqBody!!)
                                .retrieve().body(typeRef)!!
                        } else {
                            spec.retrieve().body(typeRef)!!
                        }
                    }
                    log.info { "[API-COMPARE] target api call end." }

                    // 비교 (전용 디스패처에서 수행)
                    val (n1, n2) = withContext(dispatcher) {
                        val a = objectMapper.readTree(objectMapper.writeValueAsBytes(job.sourceResult))
                        val b = objectMapper.readTree(objectMapper.writeValueAsBytes(targetRes))
                        a to b
                    }

                    if (n1 == n2) {
                        log.info { "[API-COMPARE][${job.label}] equal=true" }
                    } else {
                        val diff = JsonDiff.asJson(n1, n2)
                        log.warn { "[API-COMPARE][${job.label}] equal=false, diff=$diff" }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                log.warn(e) { "[API-COMPARE][${job.label}] skipped: target timeout (${timeoutMs}ms)" }
            } catch (e: Throwable) {
                log.warn(e) { "[API-COMPARE][${job.label}] compare failed: ${e.message}" }
            }
        }
    }
}
