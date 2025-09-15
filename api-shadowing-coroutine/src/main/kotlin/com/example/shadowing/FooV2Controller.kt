package com.example.shadowing

import com.example.shadowing.aop.Shadowing
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class FooV2Controller(
    private val v2: FooV2Service
) {

    @PostMapping("/v2/foo")
    fun postFoo(@RequestBody req: FooRequest): FooResponse {
        return v2.handle(req)
    }

    @GetMapping("/v2/foo/{id}")
    fun getFoo(@PathVariable id: Long, q: String): FooResponse {
        return v2.handle(FooRequest(id, q))
    }
}