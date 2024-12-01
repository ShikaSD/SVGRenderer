@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import androidx.compose.ui.graphics.vector.nextFloat

sealed interface PathElement {
    data class MoveTo(val x: Float, val y: Float) : PathElement
    data class LineTo(val x: Float, val y: Float) : PathElement
    data class CurveTo(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x: Float, val y: Float) : PathElement
    data object Close : PathElement
}

private inline val Long.index get() = (this ushr 32).toInt()
private inline val Long.floatValue get() = Float.fromBits((this and 0xFFFFFFFFL).toInt())
@Suppress("NOTHING_TO_INLINE")
private inline operator fun Long.component1() = floatValue
@Suppress("NOTHING_TO_INLINE")
private inline operator fun Long.component2() = index

fun parsePathData(data: String): List<PathElement> {
    if (data.isEmpty()) return emptyList()

    var offset = 0
    val segments = mutableListOf<PathElement>()
    while (offset < data.length) {
        when (data[offset]) {
            'M' -> {
                offset = skipWsp(data, offset + 1)
                val (x, endX) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endX)
                val (y, endY) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endY)
                segments.add(PathElement.MoveTo(x, y))

                offset = parseLineTo(segments, data, offset)
            }

            'L' -> {
                offset = skipWsp(data, offset + 1)
                parseLineTo(segments, data, offset)
            }

            'H' -> {
                offset = skipWsp(data, offset + 1)
                val (x, endX) = expectFloat(data, offset)
                offset = skipWsp(data, endX)
                segments.add(PathElement.LineTo(x, 0f))
            }

            'V' -> {
                offset = skipWsp(data, offset + 1)
                val (y, endY) = expectFloat(data, offset)
                offset = skipWsp(data, endY)
                segments.add(PathElement.LineTo(0f, y))
            }

            'C' -> {
                offset = skipWsp(data, offset + 1)
                val (x1, endX1) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endX1)
                val (y1, endY1) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endY1)
                val (x2, endX2) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endX2)
                val (y2, endY2) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endY2)
                val (x, endX) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endX)
                val (y, endY) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endY)
                segments.add(PathElement.CurveTo(x1, y1, x2, y2, x, y))
            }

            'Z' -> {
                offset = skipWsp(data, offset + 1)
                segments.add(PathElement.Close)
            }

            else -> error("Unknown path segment: ${data.debugString(offset)}")
        }
    }

    return segments
}

fun parseLineTo(segments: MutableList<PathElement>, data: String, start: Int): Int {
    var offset = start
    var nextFloat = nextFloat(data, offset, data.length)
    while (!nextFloat.floatValue.isNaN()) {
        val lineX = nextFloat.floatValue
        offset = skipCommaWsp(data, nextFloat.index)
        val (lineY, endLineY) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endLineY)

        segments.add(PathElement.LineTo(lineX, lineY))

        nextFloat = nextFloat(data, offset, data.length)
    }
    return offset
}

fun expectFloat(data: String, offset: Int): Long {
    val nextFloat = nextFloat(data, offset, data.length)
    if (nextFloat.floatValue.isNaN()) {
        error("Failed to parse float: ${data.debugString(offset)}")
    }
    return nextFloat
}

fun skipWsp(data: String, offset: Int): Int {
    var current = offset
    while (current < data.length) {
        when (data[current]) {
            ' ', '\n', '\t', '\r' -> current++
            else -> return current
        }
    }
    return current
}

fun skipCommaWsp(data: String, offset: Int): Int {
    var current = offset
    while(current < data.length) {
        when (data[current]) {
            ' ', '\n', '\t', '\r', ',' -> current++
            else -> return current
        }
    }
    return current
}
