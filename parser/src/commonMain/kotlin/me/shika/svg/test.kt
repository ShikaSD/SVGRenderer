package me.shika.svg

fun main () {
    val svgText = SVG.javaClass.classLoader.getResourceAsStream("sample.svg")?.reader()?.readText().orEmpty()

    val tokens = tokenize(svgText)
    val parserState = ParserState(tokens, svgText)
    val parsedRoot = parserState.parseNextElement() as ParsedTag
    val document = convertToDocument(parsedRoot)
    println(document)
}

object SVG

fun String.debugString(offset: Int) =
    buildString {
        val data = this@debugString
        append(data.substring((offset - 15).coerceAtLeast(0), offset))
        append("|")
        append(data.substring(offset, (offset + 15).coerceAtMost(data.length)))
    }
