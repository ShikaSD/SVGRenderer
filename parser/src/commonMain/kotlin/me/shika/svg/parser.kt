package me.shika.svg
sealed interface ParsedElement

data class ParsedTag(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<ParsedElement>
) : ParsedElement

data class ParsedComment(val content: String) : ParsedElement

data class ParsedText(val content: String) : ParsedElement

fun ParsedElement.print(indent: Int) {
    when (this) {
        is ParsedTag -> {
            printIndented(indent, "<$name ${attributes.entries.joinToString(" ") { (key, value) -> "$key = \"$value\"" }}>")
            children.forEach { it.print(indent + 2) }
            printIndented(indent, "</$name>")
        }
        is ParsedComment -> {
            printIndented(indent, "<!-- $content -->")
        }
        is ParsedText -> {
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
        error("$message at ${token}, text: ${svg.debugString(token.startOffset)}")
}

fun ParserState.parseNextElement(): ParsedElement? {
    val openingToken = next() ?: error("Expected a token when starting element")
    when (openingToken.type) {
        TokenType.Comment -> {
            return ParsedComment(svg.substring(openingToken.startOffset, openingToken.endOffset))
        }
        TokenType.Text -> {
            return ParsedText(svg.substring(openingToken.startOffset, openingToken.endOffset))
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
                                return ParsedTag(id, attributes, emptyList())
                            }
                            else -> {
                                parseError(next)
                            }
                        }
                        next = next() ?: parseError(token, "Incomplete tag")
                    }


                    val children = mutableListOf<ParsedElement>()
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

                    return ParsedTag(id, attributes, children)
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
