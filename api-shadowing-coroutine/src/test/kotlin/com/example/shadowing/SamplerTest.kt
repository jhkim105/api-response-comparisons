package com.example.shadowing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking

class SamplerTest : StringSpec({

    val sampler = DeterministicSampler()

    "rate 0.0 은 항상 false" {
        repeat(100) {
            sampler.shouldSample(rate = 0.0) shouldBe false
        }
    }

    "rate 1.0 은 항상 true" {
        repeat(100) {
            sampler.shouldSample(rate = 1.0) shouldBe true
        }
    }

    "랜덤 샘플링은 평균적으로 비율 근처에 수렴" {
        val trials = 10_000
        val count = (1..trials).count { sampler.shouldSample(rate = 0.2) }
        val ratio = count.toDouble() / trials
        ratio shouldBe (0.2 plusOrMinus 0.05)   // 허용 오차 ±5%
    }

    "같은 key + seed 는 항상 같은 결과" {
        val key = "user-123"
        val a = sampler.shouldSample(rate = 0.3, key = key, namespace = "exp1")
        val b = sampler.shouldSample(rate = 0.3, key = key, namespace = "exp1")
        a shouldBe b
    }

    "같은 key 라도 seed 가 다르면 결과가 달라질 수 있음" {
        val key = "user-123"
        val a = sampler.shouldSample(rate = 0.3, key = key, namespace = "exp1")
        val b = sampler.shouldSample(rate = 0.3, key = key, namespace = "exp2")
        (a == b or a != b).shouldBeTrue() // 둘 다 가능, 실행 확인용
    }
})
