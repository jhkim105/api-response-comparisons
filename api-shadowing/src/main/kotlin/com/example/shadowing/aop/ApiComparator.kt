package com.example.shadowing.aop

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.diff.JsonDiff
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

@Configuration
class CompareConfig {
    @Bean
    fun comparisonScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

@Component
class ApiComparator(
    private val objectMapper: ObjectMapper,
    @Value("\${shadowing.enabled:true}") private val enabled: Boolean,
    @Value("\${shadowing.sample-rate:1.0}") private val sampleRate: Double,
    @Value("\${shadowing.timeout-ms:1000}") private val timeoutMs: Long,
) {
    private val log = KotlinLogging.logger {}
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val random = ThreadLocalRandom.current()

    fun <Req : Any, Res : Any> compareAsync(
        apiName: String,
        req: Req,
        sourceRes: Res,
        targetCall: (Req) -> Res
    ) {
        if (!enabled) return
        if (random.nextDouble() > sampleRate) return

        coroutineScope.launch {
            try {
                withTimeout(timeoutMs) {
                    val targetRes = targetCall(req)

                    val n1 = objectMapper.readTree(objectMapper.writeValueAsBytes(sourceRes))
                    val n2 = objectMapper.readTree(objectMapper.writeValueAsBytes(targetRes))

                    if (n1 == n2) {
                        log.info { "[API-COMPARE][$apiName] equal=true" }
                    } else {
                        val diff = JsonDiff.asJson(n1, n2)
                        log.warn { "[API-COMPARE][$apiName] equal=false, diff=$diff" }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                log.warn { "[API-COMPARE][$apiName] skipped: target timeout (${timeoutMs}ms)" }
            } catch (e: Throwable) {
                log.warn(e) { "[API-COMPARE][${apiName}] compare failed: $e" }
            }
        }

    }
}