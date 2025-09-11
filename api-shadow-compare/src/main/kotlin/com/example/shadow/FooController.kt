package com.example.shadow

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class FooController(
    private val v1: FooV1Service,
    private val v2: FooV2Service,
    private val comparator: ApiComparator
) {
    @PostMapping("/v1/foo")
    fun fooV1(@RequestBody req: FooRequest): FooResponse {
        val v1Res = v1.handle(req)
        comparator.compareAsync(
            apiName = "foo",
            req = req,
            v1Res = v1Res,
            v2Call = { r -> v2.handle(r) }
        )
        return v1Res
    }

    @PostMapping("/v2/foo")
    fun fooV2(@RequestBody req: FooRequest): FooResponse =
        v2.handle(req)

    @PostMapping("/v1/foo")
    fun fooV1Sampling(@RequestBody req: FooRequest): FooResponse {
        val v1Res = v1.handle(req)
        Sampler.maybe(rate = 0.2, seed = "foo-shadow-2025") {
            comparator.compareAsync(
                apiName = "foo",
                req = req,
                v1Res = v1Res,
                v2Call = { r -> v2.handle(r) }
            )
        }
        return v1Res
    }
}
