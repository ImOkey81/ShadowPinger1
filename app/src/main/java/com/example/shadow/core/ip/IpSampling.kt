package com.example.shadow.core.ip

import kotlin.random.Random

object IpSampling {
    fun sample(values: List<Long>, ratio: Double, random: Random = Random.Default): List<Long> {
        if (ratio >= 1.0) return values
        if (ratio <= 0.0) return emptyList()
        return values.filter { random.nextDouble() <= ratio }
    }

    fun chunk(values: List<Long>, chunkSize: Int): List<List<Long>> {
        if (values.isEmpty()) return emptyList()
        val size = chunkSize.coerceAtLeast(1)
        return values.chunked(size)
    }
}
