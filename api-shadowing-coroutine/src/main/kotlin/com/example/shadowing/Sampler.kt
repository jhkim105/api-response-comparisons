package com.example.shadowing

import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.security.MessageDigest

interface Sampler {
    fun shouldSample(namespace: String = "default", key: String? = null, rate: Double): Boolean
}

@Component
class DeterministicSampler : Sampler {
    override fun shouldSample(namespace: String, key: String?, rate: Double): Boolean {
        if (rate <= 0.0) return false
        if (rate >= 1.0) return true
        val seed = (namespace + "|" + (key ?: "")).toByteArray()
        val dig = MessageDigest.getInstance("SHA-256").digest(seed)
        val v = ByteBuffer.wrap(dig).int.toLong() and 0xFFFF_FFFFL
        val max = 0x1_0000_0000L
        return v < (rate * max).toLong()
    }
}
