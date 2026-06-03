package com.youtube.rating.shared.bytearray

/**
 * Helper class to assist with building byte arrays. Works similarly to StringBuilder.
 */
class ByteArrayBuilder(initialCap: Int = 128) {

    private var buffer = ByteArray(maxOf(initialCap, 128))
    private var pos: Int = 0

    val size: Int
        get() = pos

    fun append(byte: Byte) {
        append(byteArrayOf(byte), 0..0)
    }

    fun append(array: ByteArray, srcRange: IntRange = array.indices) {
        val srcCount = maxOf(srcRange.last - srcRange.first + 1, 0)
        if (srcCount < 1) return

        val indices = array.indices
        when {
            srcRange.first !in indices ->
                throw IndexOutOfBoundsException("Range start outside array bounds")
            srcRange.last !in indices ->
                throw IndexOutOfBoundsException("Range end outside array bounds")
        }

        val newCount = pos + srcCount
        if (newCount > buffer.size) {
            val newBuff = ByteArray(maxOf(newCount, buffer.size * 2 + 2))
            buffer.copyInto(destination = newBuff, destinationOffset = 0, startIndex = 0, endIndex = pos)
            buffer = newBuff
        }

        array.copyInto(
            destination = buffer,
            destinationOffset = pos,
            startIndex = srcRange.first,
            endIndex = srcRange.first + srcCount
        )
        pos += srcCount
    }

    fun build(): ByteArray = ByteArray(pos).apply {
        buffer.copyInto(destination = this, destinationOffset = 0, startIndex = 0, endIndex = pos)
    }
}

inline fun buildByteArray(block: ByteArrayBuilder.() -> Unit): ByteArray =
    ByteArrayBuilder().apply(block).build()
