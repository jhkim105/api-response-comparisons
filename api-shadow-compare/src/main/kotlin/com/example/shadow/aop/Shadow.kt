// com.example.shadow.aop.Shadow.kt
package com.example.shadow.aop

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Shadow(
    /** 동일 요청을 POST로 보낼 v2 엔드포인트 경로 (예: "/api/v2/foo") */
    val path: String,
    /** 비교를 켜고/끄는 토글(전역 설정으로도 가능) */
    val enabled: Boolean = true
)
