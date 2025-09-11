package com.example.shadow

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking

class SamplerTest : StringSpec({

    "rate 0.0 은 항상 false" {
        repeat(100) {
            Sampler.shouldSample(0.0) shouldBe false
        }
    }

    "rate 1.0 은 항상 true" {
        repeat(100) {
            Sampler.shouldSample(1.0) shouldBe true
        }
    }

    "랜덤 샘플링은 평균적으로 비율 근처에 수렴" {
        val trials = 10_000
        val count = (1..trials).count { Sampler.shouldSample(0.2) }
        val ratio = count.toDouble() / trials
        ratio shouldBe (0.2 plusOrMinus 0.05)   // 허용 오차 ±5%
    }

    "같은 key + seed 는 항상 같은 결과" {
        val key = "user-123"
        val a = Sampler.shouldSample(0.3, key, seed = "exp1")
        val b = Sampler.shouldSample(0.3, key, seed = "exp1")
        a shouldBe b
    }

    "같은 key 라도 seed 가 다르면 결과가 달라질 수 있음" {
        val key = "user-123"
        val a = Sampler.shouldSample(0.3, key, seed = "exp1")
        val b = Sampler.shouldSample(0.3, key, seed = "exp2")
        (a == b || a != b) shouldBe true
    }

    "maybe 는 샘플되지 않으면 null 반환" {
        val result = Sampler.maybe(rate = 0.0) { "hello" }
        result.shouldBeNull()
    }

    "maybe 는 샘플되면 block 실행" {
        val result = Sampler.maybe(rate = 1.0) { "world" }
        result shouldBe "world"
    }

    "maybeSuspend 는 suspend 블록도 실행 가능" {
        val result = runBlocking {
            Sampler.maybeSuspend(rate = 1.0) {
                "suspend-ok"
            }
        }
        result shouldBe "suspend-ok"
    }
})
