// com.example.shadow.aop.ShadowAspect.kt
package com.example.shadow.aop

import com.example.shadow.ApiComparator
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Aspect
@Component
class ShadowAspect(
    private val comparator: ApiComparator,
    private val objectMapper: ObjectMapper,
    @Value("\${shadow.differential-testing.enabled:true}") private val globallyEnabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient: RestClient = RestClient.create()

    @AfterReturning(pointcut = "@annotation(shadow)", returning = "v1Result")
    fun afterReturning(joinPoint: JoinPoint, shadow: Shadow, v1Result: Any?) {
        if (!globallyEnabled || !shadow.enabled) return
        if (v1Result == null) return

        // 컨트롤러 메서드 정보
        val methodSig = joinPoint.signature as MethodSignature
        val returnType = methodSig.returnType // v1/v2 응답 타입 동일 가정
        val apiLabel = methodSig.declaringType.simpleName + "#" + methodSig.name

        // 바디 후보: HttpServletRequest/Response 등 제외한 첫 번째 인자
        val reqBody = joinPoint.args.firstOrNull {
            it != null && it !is HttpServletRequest && it !is HttpServletResponse
        } ?: run {
            log.warn("[SHADOW][{}] skip: no request body candidate found", apiLabel)
            return
        }

        // 현재 서버 base URL 구성 (http://host:port)
        val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
        val v2Url = baseUrl + shadow.path

        // 비동기 비교는 ApiComparator가 처리 (샘플링/타임아웃/격리)
        comparator.compareAsync(
            apiName = apiLabel,
            req = reqBody,
            v1Res = v1Result
        ) { r ->
            // v2에 내부 HTTP POST (동일 바디)
            restClient.post()
                .uri(v2Url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(r))
                .retrieve()
                .body(returnType)
        }
    }
}
