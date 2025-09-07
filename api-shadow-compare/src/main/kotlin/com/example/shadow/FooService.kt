package com.example.shadow

data class FooRequest(val id: Long, val q: String)
data class FooResponse(val id: Long, val value: String, val tags: List<String> = emptyList())

interface FooService {
    fun handle(req: FooRequest): FooResponse
}

@org.springframework.stereotype.Service
class FooV1Service: FooService {
    override fun handle(req: FooRequest): FooResponse {
        return FooResponse(req.id, value = "v1:${req.q}", tags = listOf("legacy"))
    }
}

@org.springframework.stereotype.Service
class FooV2Service: FooService {
    override fun handle(req: FooRequest): FooResponse {
        return FooResponse(req.id, value = "v1:${req.q}", tags = listOf("refactored"))
    }
}
