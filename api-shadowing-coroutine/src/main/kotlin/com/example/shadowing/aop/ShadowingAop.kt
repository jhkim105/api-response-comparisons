package com.example.shadowing.aop

import com.example.shadowing.Sampler
import com.example.shadowing.ShadowVerifier
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.util.UriComponentsBuilder


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Shadowing(
    val path: String
)


@Aspect
//@Order(Ordered.LOWEST_PRECEDENCE)
@Component
class ShadowAspect(
    private val verifier: ShadowVerifier,
    private val sampler: Sampler,
    @Value("\${shadowing.enabled:true}") private val enabled: Boolean,
    @Value("\${shadowing.base-url}") private val baseUrl: String,
    @Value("\${shadowing.sample-rate:1.0}") private val rate: Double,
) {
    private val log = KotlinLogging.logger {}

    @AfterReturning(
        pointcut = "@annotation(shadowing)",
        returning = "sourceResult",
        argNames = "joinPoint,shadowing,sourceResult"
    )
    fun afterReturning(joinPoint: JoinPoint, shadowing: Shadowing, sourceResult: Any?) {
        if (!enabled || sourceResult == null) return

        val ms = joinPoint.signature as MethodSignature
        val label = "${ms.declaringType.simpleName}#${ms.name}"
        val returnType = ms.method.genericReturnType

        val reqAttr = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            ?: run { log.warn("[SHADOWING][$label] skip: no request attributes"); return }
        val req = reqAttr.request
        val method = HttpMethod.valueOf(req.method)

        @Suppress("UNCHECKED_CAST")
        val pathVars = req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<String, Any?> ?: emptyMap()
        val targetUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path(shadowing.path)
            .apply { req.queryString?.let { query(it) } }
            .buildAndExpand(pathVars)
            .toUriString()

        // 필요한 헤더만 복사
        val headers = sequence {
            val names = req.headerNames
            while (names.hasMoreElements()) {
                val name = names.nextElement()
                if (name.equals("authorization", true) ||
                    name.equals("x-request-id", true) ||
                    name.equals("x-b3-traceid", true) ||
                    name.equals("x-b3-spanid", true)
                ) yield(name to (req.getHeader(name) ?: ""))
            }
        }.toMap()

        val hasBody = method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH
        val bodyArg: Any? = if (hasBody) {
            joinPoint.args.firstOrNull { it != null && it !is HttpServletRequest && it !is HttpServletResponse }
                ?: run { log.warn { "[SHADOWING][$label] skip: no request body for $method" }; return }
        } else null

        // --- 샘플링 결정 ---
        val forced = req.getHeader("X-Shadow-Force")?.lowercase()
        val key = req.getHeader("X-Shadow-Key")     // 우선순위 1: 사용자가 준 키
            ?: headers["x-request-id"]             // 우선순위 2: 요청 ID
            ?: req.requestURI                       // fallback

        val shouldRun = when (forced) {
            "on", "1", "true"  -> true
            "off", "0", "false"-> false
            else               -> sampler.shouldSample(rate = rate, key = key)
        }
        if (!shouldRun) {
            log.debug { "[SHADOWING][$label] skipped by sampling (key=$key, rate=$rate)" }
            return
        }

        // --- 비동기 제출 (어드바이스는 즉시 종료) ---
        verifier.submit(
            ShadowVerifier.Job(
                label = label,
                method = method,
                targetUrl = targetUrl,
                headers = headers,
                hasBody = hasBody,
                reqBody = bodyArg,
                sourceResult = sourceResult,
                returnType = returnType
            )
        )
    }
}
