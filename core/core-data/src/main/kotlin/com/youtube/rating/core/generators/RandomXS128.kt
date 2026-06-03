package com.youtube.rating.core.generators

import java.util.Random

class RandomXS128 : Random {

    private var seed0: Long = 0
    private var seed1: Long = 0

    constructor() {
        setSeed(seed = Random().nextLong())
    }

    constructor(seed: Long) {
        setSeed(seed = seed)
    }

    constructor(seed0: Long, seed1: Long) {
        setState(seed0 = seed0, seed1 = seed1)
    }

    override fun nextLong(): Long {
        var s1 = seed0
        val s0 = seed1
        seed0 = s0
        s1 = s1 xor (s1 shl 23)
        seed1 = s1 xor s0 xor s1.ushr(17) xor s0.ushr(26) + s0
        return seed1
    }

    override fun next(bits: Int): Int =
        (nextLong() and (1L shl bits) - 1).toInt()

    override fun nextInt(): Int = nextLong().toInt()

    override fun nextInt(n: Int): Int = nextLong(n = n.toLong()).toInt()

    override fun nextLong(n: Long): Long {
        require(n > 0) { "n must be positive" }
        while (true) {
            val bits = nextLong().ushr(1)
            val value = bits % n
            if (bits - value + (n - 1) >= 0) return value
        }
    }

    override fun nextDouble(): Double = nextLong().ushr(11) * NORM_DOUBLE

    override fun nextFloat(): Float = (nextLong().ushr(40) * NORM_FLOAT).toFloat()

    override fun nextBoolean(): Boolean = nextLong() and 1 != 0L

    override fun nextBytes(bytes: ByteArray) {
        var i = bytes.size
        while (i != 0) {
            var n = if (i < 8) i else 8
            var bits = nextLong()
            while (n-- != 0) {
                bytes[--i] = bits.toByte()
                bits = bits shr 8
            }
        }
    }

    override fun setSeed(seed: Long) {
        val s0 = murmurHash3(if (seed == 0L) Long.MIN_VALUE else seed)
        setState(seed0 = s0, seed1 = murmurHash3(s0))
    }

    fun setState(seed0: Long, seed1: Long) {
        this.seed0 = seed0
        this.seed1 = seed1
    }

    fun getState(seed: Int): Long = if (seed == 0) seed0 else seed1

    private companion object {
        private val NORM_DOUBLE = 1.0 / (1L shl 53)
        private val NORM_FLOAT = 1.0 / (1L shl 24)

        private fun murmurHash3(x: Long): Long {
            var t = x
            t = t xor t.ushr(33)
            t *= -0xae502812aa7333L
            t = t xor t.ushr(33)
            t *= -0x3b314601e57a13adL
            t = t xor t.ushr(33)
            return t
        }
    }
}
