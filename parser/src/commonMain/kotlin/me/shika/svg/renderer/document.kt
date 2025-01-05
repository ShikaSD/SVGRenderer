package me.shika.svg.renderer

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix

fun parseSvg(svg: String): SvgDocument {
    val tokens = tokenize(svg)
    val parserState = ParserState(tokens, svg)
    val parsedRoot = parserState.parseNextElement() as ParsedTag
    return convertToDocument(parsedRoot)
}

fun convertToDocument(root: ParsedTag): SvgDocument {
    root.expectName("svg")
    root.expectAttributeValue("xmlns", "http://www.w3.org/2000/svg")

    return SvgDocument(
        version = SvgDocument.Version.v1_1,
        viewBox = root.attributes["viewBox"]?.split(" ")?.let { (x, y, width, height) ->
            val xF = x.toFloat()
            val yF = y.toFloat()
            Rect(
                left = xF,
                top = yF,
                right = xF + width.toFloat(),
                bottom = yF + height.toFloat()
            )
        },
        width = root.attributes["width"]?.toFloat(),
        height = root.attributes["height"]?.toFloat(),
        children = root.children.mapNotNull {
            when (it) {
                is ParsedComment -> null
                is ParsedText -> null
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
                    "transform",
                    "fill" -> {
                    }
                    else -> {
                        error("Unknown attribute: $k")
                    }
                }
            }
            Group(
                children = parsed.children.mapNotNull {
                    when (it) {
                        is ParsedComment, is ParsedText -> null
                        is ParsedTag -> convertToElement(it)
                    }
                },
                transform = parsed.attributes["transform"]?.let { parseTransform(it) } ?: Matrix(),
                graphics = parseGraphicsElement(parsed)
            )
        }
        "path" -> {
            parsed.attributes.forEach { (k, _) ->
                when(k) {
                    "d" -> {}
                    else -> {
                        if (!checkGraphicsAttributes(k)) {
                            error("Unexpected attribute: $k")
                        }
                    }
                }
            }
            Path(
                data = parsePathData(parsed.expectAttribute("d")),
                graphics = parseGraphicsElement(parsed)
            )
        }
        "rect" -> {
            parsed.attributes.forEach { (k, _) ->
                when(k) {
                    "x",
                    "y",
                    "width",
                    "height",
                    "rx",
                    "ry" -> {}
                    else -> {
                        if (!checkGraphicsAttributes(k)) {
                            error("Unexpected attribute: $k")
                        }
                    }
                }
            }

            val x = parsed.attributes["x"]?.toFloat() ?: 0f
            val y = parsed.attributes["y"]?.toFloat() ?: 0f
            val width = parsed.expectAttribute("width").toFloat()
            val height = parsed.expectAttribute("height").toFloat()
            var rx = parsed.attributes["rx"]?.toFloat()
            var ry = parsed.attributes["ry"]?.toFloat()
            if (rx == null && ry == null) {
                rx = 0f
                ry = 0f
            } else if (rx == null) {
                rx = ry!! // for some reason Kotlin cannot infer this
            } else if (ry == null) {
                ry = rx
            }

            Rect(
                x = x,
                y = y,
                width = width,
                height = height,
                rx = rx,
                ry = ry,
                graphics = parseGraphicsElement(parsed)
            )
        }
        "circle" -> {
            parsed.attributes.forEach { (k, _) ->
                when(k) {
                    "cx",
                    "cy",
                    "r" -> {}
                    else -> {
                        if (!checkGraphicsAttributes(k)) {
                            error("Unexpected attribute: $k")
                        }
                    }
                }
            }

            Circle(
                cx = parsed.expectAttribute("cx").toFloat(),
                cy = parsed.expectAttribute("cy").toFloat(),
                r = parsed.expectAttribute("r").toFloat(),
                graphics = parseGraphicsElement(parsed)
            )
        }
        "a" -> {
            Group(
                children = parsed.children.mapNotNull {
                    when (it) {
                        is ParsedComment, is ParsedText -> null
                        is ParsedTag -> convertToElement(it)
                    }
                },
                transform = Matrix(),
                graphics = parseGraphicsElement(parsed)
            )
        }
        "defs",
        "mask",
        "title" -> null
        else -> error("Unknown tag: $parsed")
    }

private fun parseFillRule(value: String): FillRule =
    when (value) {
        "nonzero" -> FillRule.NonZero
        "evenodd" -> FillRule.EvenOdd
        else -> error("Unknown fill-rule value: $value")
    }

fun parseColor(value: String): Color {
    if (value.isEmpty()) return Color.Transparent
    if (value[0] == '#') return parseHexColor(value)
    return parseKeywordColor(value)
}

private fun parseHexColor(value: String): Color {
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

private fun parseHexDigit(c: Char): Int =
    when (c) {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> c - '0'
        'a', 'b', 'c', 'd', 'e', 'f' -> c - 'a' + 10
        'A', 'B', 'C', 'D', 'E', 'F' -> c - 'A' + 10
        else -> error("Invalid hex digit: $c")
    }

private fun parseKeywordColor(value: String): Color =
    when (value) {
        "black" -> Color.Black
        "white" -> Color.White
        "red" -> Color.Red
        "green" -> Color.Green
        "blue" -> Color.Blue
        "yellow" -> Color.Yellow
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        "gray" -> Color.Gray
        "transparent" -> Color.Transparent
        else -> error("Unknown color keyword: $value")
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

private fun checkGraphicsAttributes(attribute: String): Boolean =
    when (attribute) {
        "stroke",
        "stroke-width",
        "fill",
        "fill-rule",
        "clip-rule",
        "stroke-linecap",
        "stroke-linejoin" -> true
        else -> false
    }

private fun parseGraphicsElement(parsed: ParsedTag) =
    SvgGraphicsElement(
        stroke = parsed.attributes["stroke"]?.takeIf { it != "none" }?.let { parseColor(it) },
        strokeWidth = parsed.attributes["stroke-width"]?.toFloat(),
        strokeLineCap = parsed.attributes["stroke-linecap"]?.let {
            when (it) {
                "round" -> StrokeLineCap.Round
                "square" -> StrokeLineCap.Square
                "butt" -> StrokeLineCap.Butt
                else -> error("Unknown stroke-linecap value: $it")
            }
        },
        strokeLineJoin = parsed.attributes["stroke-linejoin"]?.let {
            when (it) {
                "round" -> StrokeLineJoin.Round
                "bevel" -> StrokeLineJoin.Bevel
                "miter" -> StrokeLineJoin.Miter
                else -> error("Unknown stroke-linejoin value: $it")
            }
        },
        fill = parsed.attributes["fill"]?.takeIf { it != "none" }?.let { parseColor(it) },
        fillRule = parsed.attributes["fill-rule"]?.let { parseFillRule(it) },
        clipRule = parsed.attributes["clip-rule"]?.let { parseFillRule(it) }
    )

interface SvgElement

data class SvgGraphicsElement(
    val stroke: Color?,
    val strokeWidth: Float?,
    val fill: Color?,
    val strokeLineCap: StrokeLineCap?,
    val strokeLineJoin: StrokeLineJoin?,
    val fillRule: FillRule?,
    val clipRule: FillRule?,
)

data class SvgDocument(
    val version: Version,
    val viewBox: Rect?,
    val width: Float?,
    val height: Float?,
    val children: List<SvgElement>,
) {
    enum class Version {
        v1_1
    }
}

data class Group(
    val children: List<SvgElement>,
    val transform: Matrix,
    val graphics: SvgGraphicsElement
) : SvgElement

data class Path(
    val data: List<PathElement>,
    val graphics: SvgGraphicsElement
) : SvgElement

data class Circle(
    val cx: Float,
    val cy: Float,
    val r: Float,
    val graphics: SvgGraphicsElement
) : SvgElement

data class Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rx: Float,
    val ry: Float,
    val graphics: SvgGraphicsElement
) : SvgElement

enum class StrokeLineCap {
    Butt,
    Round,
    Square
}

enum class StrokeLineJoin {
    Miter,
    Round,
    Bevel
}

enum class FillRule {
    EvenOdd,
    NonZero
}
