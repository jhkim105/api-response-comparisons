package com.example.shadowing

import com.example.shadowing.aop.ApiComparator
import com.example.shadowing.aop.Shadowing
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class FooController(
    private val v1: FooV1Service,
    private val v2: FooV2Service,
    private val comparator: ApiComparator
) {
    @PostMapping("/v1/foo")
    @Shadowing(path = "/api/v2/foo")
    fun postFoo(@RequestBody req: FooRequest): FooResponse {
        return v1.handle(req)
    }

    @GetMapping("/v1/foo/{id}")
    @Shadowing(path = "/api/v2/foo/{id}")
    fun getFoo(@PathVariable id: Long, q: String): FooResponse {
        return v1.handle(FooRequest(id, q))
    }

}