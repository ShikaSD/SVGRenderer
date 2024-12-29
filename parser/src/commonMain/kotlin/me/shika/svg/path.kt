@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.shika.svg

import androidx.compose.ui.graphics.vector.nextFloat

sealed interface PathElement {
    data class MoveTo(
        val x: Float,
        val y: Float,
        val relative: Boolean,
    ) : PathElement
    data class LineTo(
        val x: Float,
        val y: Float,
        val relative: Boolean
    ) : PathElement
    data class CurveTo(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val x: Float,
        val y: Float,
        val relative: Boolean
    ) : PathElement
    data class ArcTo(
        val rx: Float,
        val ry: Float,
        val rotation: Float,
        val largeArc: Boolean,
        val sweep: Boolean,
        val x: Float,
        val y: Float,
        val relative: Boolean
    ) : PathElement
    data class QuadTo(
        val x1: Float,
        val y1: Float,
        val x: Float,
        val y: Float,
        val relative: Boolean
    ) : PathElement
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
        when (val op = data[offset]) {
            'M', 'm' -> {
                offset = skipWsp(data, offset + 1)
                val (x, endX) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endX)
                val (y, endY) = expectFloat(data, offset)
                offset = skipCommaWsp(data, endY)
                segments.add(PathElement.MoveTo(x, y, relative = op == 'm'))

                offset = parseLineTo(segments, data, offset, relative = op == 'm')
            }

            'L', 'l' -> {
                offset = skipWsp(data, offset + 1)
                offset = parseLineTo(segments, data, offset, relative = op == 'l')
            }

            'H', 'h' -> {
                offset = skipWsp(data, offset + 1)
                val (x, endX) = expectFloat(data, offset)
                offset = skipWsp(data, endX)
                segments.add(PathElement.LineTo(x, Float.NaN, relative = op == 'h'))
            }

            'V', 'v' -> {
                offset = skipWsp(data, offset + 1)
                val (y, endY) = expectFloat(data, offset)
                offset = skipWsp(data, endY)
                segments.add(PathElement.LineTo(Float.NaN, y, relative = op == 'v'))
            }

            'C', 'c' -> {
                offset = skipWsp(data, offset + 1)
                offset = parseCurveTo(segments, data, offset, relative = op == 'c')
            }

            'S', 's' -> {
                offset = skipWsp(data, offset + 1)
                offset = parseSmoothCurveTo(segments, data, offset, relative = op == 's')
            }

            'A', 'a' -> {
                offset = skipWsp(data, offset + 1)
                offset = parseArc(segments, data, offset, relative = op == 'a')
            }

            'Q', 'q' -> {
                offset = skipWsp(data, offset + 1)
                offset = parseQuadTo(segments, data, offset, relative = op == 'q')
            }

            'T', 't' -> {
                offset = skipWsp(data, offset + 1)
                offset = parseSmoothQuadTo(segments, data, offset, relative = op == 't')
            }

            'Z' -> {
                offset = skipWsp(data, offset + 1)
                segments.add(PathElement.Close)
            }

            else -> error("Unexpected path segment: ${data.debugString(offset)}")
        }
    }

    return segments
}

private fun parseLineTo(segments: MutableList<PathElement>, data: String, start: Int, relative: Boolean): Int {
    var offset = start
    var nextFloat = nextFloat(data, offset, data.length)
    while (!nextFloat.floatValue.isNaN()) {
        val lineX = nextFloat.floatValue
        offset = skipCommaWsp(data, nextFloat.index)
        val (lineY, endLineY) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endLineY)

        segments.add(PathElement.LineTo(lineX, lineY, relative))

        nextFloat = nextFloat(data, offset, data.length)
    }
    return offset
}

private fun parseArc(segments: MutableList<PathElement>, data: String, start: Int, relative: Boolean): Int {
    var offset = start
    var nextFloat = nextFloat(data, offset, data.length)
    while (!nextFloat.floatValue.isNaN()) {
        val rx = nextFloat.floatValue
        offset = skipCommaWsp(data, nextFloat.index)
        val (ry, endRy) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endRy)
        val (rotation, endRotation) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endRotation)
        val largeArc = expectFlag(data, offset)
        offset = skipCommaWsp(data, offset + 1)
        val sweep = expectFlag(data, offset)
        offset = skipCommaWsp(data, offset + 1)
        val (x, endX) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endX)
        val (y, endY) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endY)

        if (rx == 0f || ry == 0f) {
            segments.add(PathElement.LineTo(x, y, relative))
        } else {
            segments.add(
                PathElement.ArcTo(rx, ry, rotation, largeArc, sweep, x, y, relative)
            )
        }

        nextFloat = nextFloat(data, offset, data.length)
    }
    return offset
}

private fun parseCurveTo(segments: MutableList<PathElement>, data: String, start: Int, relative: Boolean): Int {
    var offset = start
    var nextFloat = nextFloat(data, offset, data.length)
    while (!nextFloat.floatValue.isNaN()) {
        val x1 = nextFloat.floatValue
        offset = skipCommaWsp(data, nextFloat.index)
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

        segments.add(PathElement.CurveTo(x1, y1, x2, y2, x, y, relative))

        nextFloat = nextFloat(data, offset, data.length)
    }
    return offset
}

private fun parseSmoothCurveTo(segments: MutableList<PathElement>, data: String, start: Int, relative: Boolean): Int {
    var offset = start
    var nextFloat = nextFloat(data, offset, data.length)
    while (!nextFloat.floatValue.isNaN()) {
        val x2 = nextFloat.floatValue
        offset = skipCommaWsp(data, nextFloat.index)
        val (y2, endY2) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endY2)
        val (x, endX) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endX)
        val (y, endY) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endY)

        segments.add(PathElement.CurveTo(Float.NaN, Float.NaN, x2, y2, x, y, relative))

        nextFloat = nextFloat(data, offset, data.length)
    }
    return offset
}

private fun parseQuadTo(segments: MutableList<PathElement>, data: String, start: Int, relative: Boolean): Int {
    var offset = start
    var nextFloat = nextFloat(data, offset, data.length)
    while (!nextFloat.floatValue.isNaN()) {
        val x1 = nextFloat.floatValue
        offset = skipCommaWsp(data, nextFloat.index)
        val (y1, endY1) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endY1)
        val (x, endX) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endX)
        val (y, endY) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endY)

        segments.add(PathElement.QuadTo(x1, y1, x, y, relative))

        nextFloat = nextFloat(data, offset, data.length)
    }
    return offset
}

private fun parseSmoothQuadTo(segments: MutableList<PathElement>, data: String, start: Int, relative: Boolean): Int {
    var offset = start
    var nextFloat = nextFloat(data, offset, data.length)
    while (!nextFloat.floatValue.isNaN()) {
        val x = nextFloat.floatValue
        offset = skipCommaWsp(data, nextFloat.index)
        val (y, endY) = expectFloat(data, offset)
        offset = skipCommaWsp(data, endY)

        segments.add(PathElement.QuadTo(Float.NaN, Float.NaN, x, y, relative))

        nextFloat = nextFloat(data, offset, data.length)
    }
    return offset
}

private fun expectFloat(data: String, offset: Int): Long {
    val nextFloat = nextFloat(data, offset, data.length)
    if (nextFloat.floatValue.isNaN()) {
        error("Failed to parse float: ${data.debugString(offset)}")
    }
    return nextFloat
}

private fun expectFlag(data: String, offset: Int): Boolean =
    when (data[offset]) {
        '0' -> false
        '1' -> true
        else -> error("Expected flag value at ${data.debugString(offset)}")
    }

private fun skipWsp(data: String, offset: Int): Int {
    var current = offset
    while (current < data.length) {
        when (data[current]) {
            ' ', '\n', '\t', '\r' -> current++
            else -> return current
        }
    }
    return current
}

private fun skipCommaWsp(data: String, offset: Int): Int {
    var current = offset
    while(current < data.length) {
        when (data[current]) {
            ' ', '\n', '\t', '\r', ',' -> current++
            else -> return current
        }
    }
    return current
}
