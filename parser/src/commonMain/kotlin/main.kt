import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix

fun main () {
    val svgText = SVG.javaClass.classLoader.getResourceAsStream("sample.svg")?.reader()?.readText().orEmpty()

    val tokens = tokenize(svgText)
    val parserState = ParserState(tokens, svgText)
    val parsedRoot = parserState.parseNextElement() as ParsedTag
    val document = convertToDocument(parsedRoot)
    println(document)
}

object SVG

fun convertToDocument(root: ParsedTag): Document {
    root.expectName("svg")
    root.expectAttributeValue("version", "1.1")
    root.expectAttributeValue("xmlns", "http://www.w3.org/2000/svg")

    return Document(
        version = Document.Version.v1_1,
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
        width = root.expectAttribute("width").toFloat(),
        height = root.expectAttribute("height").toFloat(),
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

fun convertToElement(parsed: ParsedTag): Element? =
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
                    "round" -> Group.StrokeLineCap.Round
                    "square" -> Group.StrokeLineCap.Square
                    "butt", null -> Group.StrokeLineCap.Butt
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
                    "fill" -> {}
                    else -> {
                        error("Unknown attribute: $k")
                    }
                }
            }
            Path(
                data = parsePathData(parsed.expectAttribute("d")),
                strokeColor = parseColor(parsed.expectAttribute("stroke")),
                strokeWidth = parsed.expectAttribute("stroke-width").toFloat(),
                fill = parsed.attributes["fill"]?.takeIf { it != "none" }?.let { parseColor(it) }
            )
        }
        "defs",
        "mask" -> null
        else -> error("Unknown tag: $parsed")
    }


@OptIn(ExperimentalStdlibApi::class)
fun parseColor(value: String): Color {
    require(value.startsWith("#")) { "Color value should start with #, but got $value" }
    return Color(value.substring(1).hexToLong())
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
                rotateX(parsedValues[0])
                rotateY(parsedValues[1])
                rotateZ(parsedValues[2])
            }
            else -> error("Unknown transform operation: $operation")
        }
    }
}

interface Element

data class Document(
    val version: Version,
    val viewBox: Rect,
    val width: Float,
    val height: Float,
    val children: List<Element>
) {
    enum class Version {
        v1_1
    }
}

data class Group(
    val children: List<Element>,
    val strokeLineCap: StrokeLineCap = StrokeLineCap.Butt,
    val transform: Matrix
) : Element {
    enum class StrokeLineCap {
        Butt,
        Round,
        Square
    }
}

data class Path(
    val data: List<PathElement>,
    val strokeColor: Color,
    val strokeWidth: Float,
    val fill: Color?,
) : Element


fun String.debugString(offset: Int) =
    buildString {
        val data = this@debugString
        append(data.substring((offset - 15).coerceAtLeast(0), offset))
        append("|")
        append(data.substring(offset, (offset + 15).coerceAtMost(data.length)))
    }
