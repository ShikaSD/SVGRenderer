fun main () {
    val svgText = SVG.javaClass.classLoader.getResourceAsStream("sample.svg")?.reader()?.readText().orEmpty()

    val tokens = tokenize(svgText)
    val parserState = ParserState(tokens, svgText)
    val element = parserState.parseNextElement()
    element?.print(0)
}

object SVG


sealed interface Element

data class Tag(
    val id: String,
    val attributes: Map<String, String>,
    val children: List<Element>
) : Element

data class Comment(val content: String) : Element

private fun Element.print(indent: Int) {
    when (this) {
        is Tag -> {
            printIndented(indent, "<$id ${attributes.entries.joinToString(" ") { (key, value) -> "$key = \"$value\"" }}>")
            children.forEach { it.print(indent + 2) }
            printIndented(indent, "</$id>")
        }
        is Comment -> {
            printIndented(indent, content)
        }
    }
}

fun printIndented(indent: Int, string: String) {
    repeat(indent) {
        print(" ")
    }
    println(string)
}

class ParserState(val tokens: List<Token>, val svg: String) {
    private var offset = 0

    fun next(): Token? {
        if (offset >= tokens.size) {
            return null
        }
        return tokens[offset++]
    }

    fun expect(tokenType: TokenType): Token {
        val token = next() ?: error("Expected token of type $tokenType")
        if (token.type != tokenType) {
            parseError(token, "Expected token of type $tokenType")
        }
        return token
    }

    fun rewind() {
        offset--
    }

    fun parseError(token: Token, message: String = "Unexpected token"): Nothing =
        error("$message at ${token}, text: ${svg.substring(token.startOffset, token.endOffset + 10)}")
}

fun ParserState.parseNextElement(): Element? {

    val openingToken = next() ?: error("Expected a token when starting element")
    when (openingToken.type) {
        TokenType.Comment -> {
            return Comment(svg.substring(openingToken.startOffset, openingToken.endOffset))
        }
        TokenType.AngleBracketOpen -> {
            val token = next() ?: parseError(openingToken, "Expected a token after ")
            when (token.type) {
                TokenType.ForwardSlash -> {
                    // </
                    //  ^ we are here
                    rewind()
                    rewind()
                    return null // Closing tag, return to parent
                }
                TokenType.Identifier -> {
                    val id = svg.substring(token.startOffset, token.endOffset)
                    val attributes = mutableMapOf<String, String>()
                    var next = next() ?: parseError(token, "Incomplete tag")
                    while (next.type != TokenType.AngleBracketClose) {
                        when (next.type) {
                            TokenType.Identifier -> {
                                val key = svg.substring(next.startOffset, next.endOffset)
                                expect(TokenType.Equal)
                                val valueToken = expect(TokenType.StringLiteral)

                                attributes[key] = svg.substring(valueToken.startOffset + 1, valueToken.endOffset - 1)
                            }
                            TokenType.ForwardSlash -> {
                                // Self closing tag
                                expect(TokenType.AngleBracketClose)
                                return Tag(id, attributes, emptyList())
                            }
                            else -> {
                                parseError(next)
                            }
                        }
                        next = next() ?: parseError(token, "Incomplete tag")
                    }


                    val children = mutableListOf<Element>()
                    var child = parseNextElement()
                    while (child != null) {
                        children.add(child)
                        child = parseNextElement()
                    }

                    expect(TokenType.AngleBracketOpen)
                    expect(TokenType.ForwardSlash)
                    val closingId = expect(TokenType.Identifier)
                    if (id != svg.substring(closingId.startOffset, closingId.endOffset)) {
                        parseError(closingId, "Expected closing tag for $id")
                    }
                    expect(TokenType.AngleBracketClose)

                    return Tag(id, attributes, children)
                }
                else -> {
                    parseError(token)
                }
            }
        }
        else -> {
            parseError(openingToken)
        }
    }

    return null
}
