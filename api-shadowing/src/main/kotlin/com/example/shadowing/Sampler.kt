package com.example.shadowing

import java.util.concurrent.ThreadLocalRandom
import java.util.zip.CRC32
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Sampler {
    /** 비율(0.0~1.0)과 선택적 key/seed로 샘플링; 샘플되면 block 실행, 아니면 null 반환 */
    fun <T> maybe(rate: Double, key: String? = null, seed: String = "default", block: () -> T): T? {
        if (!shouldSample(rate, key, seed)) return null
        return block()
    }

    /** suspend 버전: 샘플되면 block 실행 */
    suspend fun <T> maybeSuspend(
        rate: Double,
        key: String? = null,
        seed: String = "default",
        block: suspend () -> T
    ): T? {
        if (!shouldSample(rate, key, seed)) return null
        return withContext(Dispatchers.IO) { block() }
    }

    /** 샘플 여부만 알고 싶을 때 */
    fun shouldSample(rate: Double, key: String? = null, seed: String = "default"): Boolean {
        val r = rate.coerceIn(0.0, 1.0)
        if (r <= 0.0) return false
        if (r >= 1.0) return true
        return if (key == null) {
            ThreadLocalRandom.current().nextDouble() < r
        } else {
            val crc = CRC32().apply {
                update(seed.toByteArray())
                update(0)
                update(key.toByteArray())
            }.value
            val bucket = (crc % 10_000).toInt()           // 0..9999
            bucket < (r * 10_000).toInt()                 // e.g., 0.2 -> 2000
        }
    }
}
