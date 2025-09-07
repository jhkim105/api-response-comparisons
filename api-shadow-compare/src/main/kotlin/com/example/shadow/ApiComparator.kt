package com.example.shadow

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class CompareConfig {
    @Bean
    fun comparisonScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

/**
 * ApiComparator
 *
 * Strategy: Shadow Deployment + Differential Testing (Back-to-Back)
 */
@Component
class ApiComparator(
    private val objectMapper: ObjectMapper,
    private val scope: CoroutineScope,
    @Value("\${shadow.differential-testing.enabled:true}") private val enabled: Boolean,
    @Value("\${shadow.differential-testing.sample-rate:1.0}") private val sampleRate: Double,
    @Value("\${shadow.differential-testing.timeout-ms:1000}") private val timeoutMs: Long,
) {
    private val log = LoggerFactory.getLogger("shadow.compare")
    private val random = java.util.concurrent.ThreadLocalRandom.current()

    fun <Req : Any, Res : Any> compareAsync(
        apiName: String,
        req: Req,
        v1Res: Res,
        v2Call: suspend (Req) -> Res
    ) {
        if (!enabled) return
        if (random.nextDouble() > sampleRate) return

        scope.launch {
            try {
                withTimeout(timeoutMs) {
                    val v2Res = v2Call(req)

                    val n1 = objectMapper.readTree(objectMapper.writeValueAsBytes(v1Res))
                    val n2 = objectMapper.readTree(objectMapper.writeValueAsBytes(v2Res))

                    if (n1 == n2) {
                        log.info("[API-COMPARE][{}] equal=true", apiName)
                    } else {
                        val diff = com.github.fge.jsonpatch.diff.JsonDiff.asJson(n1, n2)
                        log.warn("[API-COMPARE][{}] equal=false, diff={}", apiName, diff.toString())
                    }
                }
            } catch (t: TimeoutCancellationException) {
                log.warn("[API-COMPARE][{}] skipped: v2 timeout ({}ms)", apiName, timeoutMs)
            } catch (t: Throwable) {
                log.warn("[API-COMPARE][{}] compare failed: {}", apiName, t.toString())
            }
        }
    }
}
