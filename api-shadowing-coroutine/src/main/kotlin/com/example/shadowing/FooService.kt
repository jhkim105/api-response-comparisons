package com.example.shadowing

import org.springframework.stereotype.Service

data class FooRequest(val id: Long, val q: String)
data class FooResponse(val id: Long, val value: String)

interface FooService {
    fun handle(req: FooRequest): FooResponse
}

@Service
class FooV1Service: FooService {
    override fun handle(req: FooRequest): FooResponse {
        return FooResponse(req.id, value = "value of ${req.q}")
    }
}

@Service
class FooV2Service: FooService {
    override fun handle(req: FooRequest): FooResponse {
        return FooResponse(req.id, value = "value of ${req.q}")
    }
}
