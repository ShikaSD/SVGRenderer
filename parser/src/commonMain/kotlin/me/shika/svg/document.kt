package me.shika.svg

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix


fun convertToDocument(root: ParsedTag): SvgDocument {
    root.expectName("svg")
    root.expectAttributeValue("xmlns", "http://www.w3.org/2000/svg")

    return SvgDocument(
        version = SvgDocument.Version.v1_1,
        viewBox = root.expectAttribute("viewBox").split(" ").let { (x, y, width, height) ->
            val xF = x.toFloat()
            val yF = y.toFloat()
            Rect(
                left = xF,
                top = yF,
                right = xF + width.toFloat(),
                bottom = yF + height.toFloat()
            )
        },
        children = root.children.mapNotNull {
            when (it) {
                is ParsedComment -> null
                is ParsedTag -> convertToElement(it)
            }
        }
    )
}

fun ParsedTag.expectName(name: String) {
    if (this.name != name) {
        error("Expected tag with name $name, but got $this")
    }
}

fun ParsedTag.expectAttribute(name: String): String {
    return attributes[name] ?: error("Expected tag with attribute $name, but got $this")
}

fun ParsedTag.expectAttributeValue(name: String, value: String) {
    if (attributes[name] != value) {
        error("Expected tag attribute $name to be $value, but got $this")
    }
}

private fun convertToElement(parsed: ParsedTag): SvgElement? =
    when(parsed.name) {
        "g" -> {
            parsed.attributes.forEach { (k, _) ->
                when(k) {
                    "stroke-linecap",
                    "transform" -> {}
                    else -> {
                        error("Unknown attribute: $k")
                    }
                }
            }
            Group(
                children = parsed.children.mapNotNull {
                    when (it) {
                        is ParsedComment -> null
                        is ParsedTag -> convertToElement(it)
                    }
                },
                strokeLineCap = when (parsed.attributes["stroke-linecap"]) {
                    "round" -> StrokeLineCap.Round
                    "square" -> StrokeLineCap.Square
                    "butt" -> StrokeLineCap.Butt
                    null -> null
                    else -> error("Unknown stroke-linecap value: $parsed")
                },
                transform = parsed.attributes["transform"]?.let { parseTransform(it) } ?: Matrix()
            )
        }
        "path" -> {
            parsed.attributes.forEach { (k, _) ->
                when(k) {
                    "d",
                    "stroke",
                    "stroke-width",
                    "fill",
                    "fill-rule",
                    "clip-rule" -> {}
                    else -> {
                        error("Unknown attribute: $k")
                    }
                }
            }
            Path(
                data = parsePathData(parsed.expectAttribute("d")),
                stroke = parsed.attributes["stroke"]?.takeIf { it != "none" }?.let { parseColor(it) },
                strokeWidth = parsed.attributes["stroke-width"]?.toFloat() ?: 1f,
                fill = parsed.attributes["fill"]?.takeIf { it != "none" }?.let { parseColor(it) },
                fillRule = parsed.attributes["fill-rule"]?.let { parseFillRule(it) },
                clipRule = parsed.attributes["clip-rule"]?.let { parseFillRule(it) }
            )
        }
        "defs",
        "mask" -> null
        else -> error("Unknown tag: $parsed")
    }

private fun parseFillRule(value: String): FillRule =
    when (value) {
        "nonzero" -> FillRule.NonZero
        "evenodd" -> FillRule.EvenOdd
        else -> error("Unknown fill-rule value: $value")
    }

@OptIn(ExperimentalStdlibApi::class)
fun parseColor(value: String): Color {
    require(value.startsWith("#")) { "Expected a hex string, but got $value" }
    when (value.length) {
        7 -> {
            val r1 = parseHexDigit(value[1])
            val r2 = parseHexDigit(value[2])
            val g1 = parseHexDigit(value[3])
            val g2 = parseHexDigit(value[4])
            val b1 = parseHexDigit(value[5])
            val b2 = parseHexDigit(value[6])
            return Color(
                (r1 shl 4) or r2,
                (g1 shl 4) or g2,
                (b1 shl 4) or b2,
            )
        }
        4 -> {
            val r = parseHexDigit(value[1])
            val g = parseHexDigit(value[2])
            val b = parseHexDigit(value[3])
            return Color(
                (r shl 4) or r,
                (g shl 4) or g,
                (b shl 4) or b,
            )
        }
        else -> error("Unknown color format: $value")
    }
}

fun parseHexDigit(c: Char): Int =
    when (c) {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> c - '0'
        'a', 'b', 'c', 'd', 'e', 'f' -> c - 'a' + 10
        'A', 'B', 'C', 'D', 'E', 'F' -> c - 'A' + 10
        else -> error("Invalid hex digit: $c")
    }

fun parseTransform(value: String): Matrix {
    val transformRegex = "([a-z]+)\\(([0-9. ]+)\\)".toRegex()
    val matches = transformRegex.findAll(value)
    return matches.fold(Matrix()) { matrix, match ->
        val (operation, values) = match.destructured
        val parsedValues = values.split(" ").map { it.toFloat() }
        when (operation) {
            "translate" -> matrix.apply {
                translate(parsedValues[0], parsedValues[1])
            }
            "scale" -> matrix.apply {
                scale(parsedValues[0], parsedValues.getOrNull(1) ?: parsedValues[0])
            }
            "rotate" -> matrix.apply {
                if (parsedValues.size == 3) {
                    val cx = parsedValues[1]
                    val cy = parsedValues[2]
                    translate(cx, cy)
                    rotateZ(parsedValues[0])
                    translate(-cx, -cy)
                } else {
                    rotateZ(parsedValues[0])
                }
            }
            else -> error("Unknown transform operation: $operation")
        }
    }
}

interface SvgElement

data class SvgDocument(
    val version: Version,
    val viewBox: Rect,
    val children: List<SvgElement>
) {
    enum class Version {
        v1_1
    }
}

data class Group(
    val children: List<SvgElement>,
    val strokeLineCap: StrokeLineCap?,
    val transform: Matrix
) : SvgElement

data class Path(
    val data: List<PathElement>,
    val stroke: Color?,
    val strokeWidth: Float,
    val fill: Color?,
    val fillRule: FillRule?,
    val clipRule: FillRule?
) : SvgElement

enum class StrokeLineCap {
    Butt,
    Round,
    Square
}

enum class FillRule {
    EvenOdd,
    NonZero
}
