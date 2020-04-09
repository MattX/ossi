// Copyright 2020 Matthieu Felix
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
        private const val EXPECTED_INSERTIONS = 2000
        private const val ERROR_RATE = 0.05

        fun new() = VoterBloomFilter(BloomFilter.create(VoterFunnel, EXPECTED_INSERTIONS, ERROR_RATE))
    }
}
