package com.example.shadowing.aop

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.util.UriComponentsBuilder
import java.lang.reflect.Type

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Shadowing(
    val path: String
)

@Aspect
@Component
class ShadowAspect(
    private val comparator: ApiComparator,
    private val objectMapper: ObjectMapper,
    @Value("\${shadowing.enabled:true}") private val enabled: Boolean,
    @Value("\${shadowing.base-url}") private val baseUrl: String,
) {
    private val log = KotlinLogging.logger {}
    private val restClient: RestClient = RestClient.builder()
        .messageConverters {
            it.removeIf { converter -> converter is MappingJackson2HttpMessageConverter }
            it.add(MappingJackson2HttpMessageConverter(objectMapper))
        }
        .build()


    @AfterReturning(pointcut = "@annotation(shadowing)", returning = "sourceResult")
    fun afterReturning(joinPoint: JoinPoint, shadowing: Shadowing, sourceResult: Any?) {
        if (!enabled || sourceResult == null) return

        val methodSig = joinPoint.signature as MethodSignature
        val returnGenericType: Type = methodSig.method.genericReturnType
        val returnTypeRef = object : ParameterizedTypeReference<Any>() {
            override fun getType(): Type = returnGenericType
        }

        val apiLabel = "${methodSig.declaringType.simpleName}#${methodSig.name}"

        val reqAttr = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: run {
            log.warn("[SHADOWING][{}] skip: no request attributes", apiLabel); return
        }
        val servletReq = reqAttr.request
        val httpMethod = HttpMethod.valueOf(servletReq.method) // 실제 요청 메서드

        @Suppress("UNCHECKED_CAST")
        val pathVars: Map<String, Any?> =
            servletReq.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<String, Any?> ?: emptyMap()
        val queryString = servletReq.queryString

        val targetUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path(shadowing.path) // 템플릿
            .apply { if (queryString != null) query(queryString) }
            .buildAndExpand(pathVars)
            .toUriString()

        // 전송할 헤더 화이트리스트(필요 시 추가)
        val headers: Map<String, String> = sequence {
            val names = servletReq.headerNames
            while (names.hasMoreElements()) {
                val name = names.nextElement()
                if (name.equals("authorization", true) ||
                    name.equals("x-request-id", true) ||
                    name.equals("x-b3-traceid", true) ||
                    name.equals("x-b3-spanid", true)
                ) yield(name to (servletReq.getHeader(name) ?: ""))
            }
        }.toMap()

        // 본문이 있는 메서드 여부
        val hasBody = when (httpMethod) {
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH -> true
            else -> false
        }

        // 바디 후보 추출 (본문 없는 메서드는 Unit 사용)
        val reqBody: Any = if (hasBody) {
            joinPoint.args.firstOrNull {
                it != null && it !is HttpServletRequest && it !is HttpServletResponse
            } ?: run {
                log.warn {"[SHADOWING][$apiLabel] skip: no request body for $httpMethod"}
                return
            }
        } else Unit

        // 비교 실행
        if (hasBody) {
            comparator.compareAsync(
                apiName = apiLabel,
                req = reqBody,
                sourceRes = sourceResult
            ) { r ->
                var spec = restClient.method(httpMethod).uri(targetUrl)
                headers.forEach { (k, v) -> spec = spec.header(k, v) }
                spec.contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(r))
                    .retrieve()
                    .body(returnTypeRef)!!
            }
        } else {
            comparator.compareAsync(
                apiName = apiLabel,
                req = Unit,
                sourceRes = sourceResult
            ) {
                var spec = restClient.method(httpMethod).uri(targetUrl)
                headers.forEach { (k, v) -> spec = spec.header(k, v) }
                spec.retrieve().body(returnTypeRef)!!
            }
        }
    }
}