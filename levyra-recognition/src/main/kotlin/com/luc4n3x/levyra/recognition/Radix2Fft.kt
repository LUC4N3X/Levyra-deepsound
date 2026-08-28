package com.luc4n3x.levyra.recognition

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class Radix2Fft(private val size: Int) {
    private val cosine = DoubleArray(size / 2) { index -> cos(2.0 * PI * index / size) }
    private val sine = DoubleArray(size / 2) { index -> sin(2.0 * PI * index / size) }
    private val bitReversed = IntArray(size).also { table ->
        val bits = Integer.numberOfTrailingZeros(size)
        for (index in 0 until size) table[index] = reverseBits(index, bits)
    }

    init {
        require(size > 1 && size and (size - 1) == 0)
    }

    fun transform(real: DoubleArray, imaginary: DoubleArray) {
        require(real.size == size && imaginary.size == size)
        for (index in 0 until size) {
            val mirror = bitReversed[index]
            if (mirror <= index) continue
            val realValue = real[index]
            real[index] = real[mirror]
            real[mirror] = realValue
            val imaginaryValue = imaginary[index]
            imaginary[index] = imaginary[mirror]
            imaginary[mirror] = imaginaryValue
        }

        var width = 2
        while (width <= size) {
            val halfWidth = width / 2
            val twiddleStep = size / width
            var blockStart = 0
            while (blockStart < size) {
                var offset = 0
                var twiddleIndex = 0
                while (offset < halfWidth) {
                    val low = blockStart + offset
                    val high = low + halfWidth
                    val highReal = real[high]
                    val highImaginary = imaginary[high]
                    val rotatedReal = highReal * cosine[twiddleIndex] + highImaginary * sine[twiddleIndex]
                    val rotatedImaginary = -highReal * sine[twiddleIndex] + highImaginary * cosine[twiddleIndex]

                    real[high] = real[low] - rotatedReal
                    imaginary[high] = imaginary[low] - rotatedImaginary
                    real[low] += rotatedReal
                    imaginary[low] += rotatedImaginary

                    offset++
                    twiddleIndex += twiddleStep
                }
                blockStart += width
            }
            width = width shl 1
        }
    }

    private fun reverseBits(value: Int, bitCount: Int): Int {
        var source = value
        var target = 0
        repeat(bitCount) {
            target = (target shl 1) or (source and 1)
            source = source ushr 1
        }
        return target
    }
}
