package io.terbium.ossi

import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnel
import com.google.common.hash.PrimitiveSink
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class VoterBloomFilter private constructor(private val filter: BloomFilter<String>) {
    constructor(bytes: ByteArray) : this(BloomFilter.readFrom(ByteArrayInputStream(bytes), VoterFunnel))

    object VoterFunnel : Funnel<String> {
        override fun funnel(from: String, into: PrimitiveSink) {
            into.putString(from, Charsets.UTF_8)
        }
    }

    fun save(): ByteArray {
        val os = ByteArrayOutputStream()
        filter.writeTo(os);
        return os.toByteArray()
    }

    fun mightContain(s: String): Boolean = filter.mightContain(s)

    fun put(s: String): Boolean = filter.put(s)

    companion object {
        const val EXPECTED_INSERTIONS = 2000
        const val ERROR_RATE = 0.05

        fun new() = VoterBloomFilter(BloomFilter.create(VoterFunnel, EXPECTED_INSERTIONS, ERROR_RATE))
    }
}
