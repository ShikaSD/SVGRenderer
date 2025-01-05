package me.shika.svg.renderer

data class Token(
    val type: TokenType,
    // Inclusive
    val startOffset: Int,
    // Exclusive
    val endOffset: Int
)

enum class TokenType {
    AngleBracketOpen,
    AngleBracketClose,
    ForwardSlash,
    Identifier,
    Equal,
    StringLiteral,
    Comment,
    Text,
    Eof
}
fun tokenize(svg: String): List<Token> = buildList {
    var offset = 0
    var insideTag = false
    while (offset < svg.length) {
        when (svg[offset]) {
            '<' -> {
                when(svg[offset + 1]) {
                    '!' -> {
                        when(svg[offset + 2]) {
                            '-' -> {
                                val token = comment(offset, svg)
                                offset = token.endOffset - 1
                                add(token)
                            }
                            'd', 'D' -> {
                                // todo: validate doctype is svg
                                // skip doctype
                                while (offset < svg.length) {
                                    if (svg[offset] == '>') {
                                        break
                                    }
                                    offset++
                                }
                            }
                            else -> lexerError(offset, svg)
                        }
                    }
                    '?' -> {
                        // Skip XML prolog
                        offset += 2
                        while (offset < svg.length - 1) {
                            if (svg[offset] == '?' && svg[offset + 1] == '>') {
                                offset++
                                break
                            }
                            offset++
                        }
                    }
                    else -> {
                        insideTag = true
                        add(Token(TokenType.AngleBracketOpen, offset, offset + 1))
                    }
                }
            }
            '>' -> {
                insideTag = false
                add(Token(TokenType.AngleBracketClose, offset, offset + 1))
            }
            '/' -> add(Token(TokenType.ForwardSlash, offset, offset + 1))
            '=' -> add(Token(TokenType.Equal, offset, offset + 1))
            '"' -> {
                val start = offset
                offset++
                while (offset < svg.length && svg[offset] != '"') {
                    offset++
                }
                if (offset == svg.length) {
                    error("Unterminated string literal")
                }
                add(Token(TokenType.StringLiteral, start, offset + 1))
            }

            ' ',
            '\n',
            '\t' -> {
                offset++
                while (offset < svg.length) {
                    when(svg[offset]) {
                        ' ',
                        '\n',
                        '\t' -> {
                            offset++
                        }

                        else -> break
                    }
                }
                offset-- // we are at the first non-whitespace character
            }
            else -> {
                if (insideTag) {
                    val identifier = identifier(offset, svg)
                    if (identifier != null) {
                        add(identifier)
                        offset = identifier.endOffset - 1
                    } else {
                        lexerError(offset, svg)
                    }
                } else {
                    val start = offset
                    while (offset < svg.length) {
                        if (svg[offset] == '<') {
                            break
                        }
                        offset++
                    }
                    add(Token(TokenType.Text, start, offset))
                    offset-- // we are at the opening angle bracket
                }
            }
        }
        offset++
    }
    add(Token(TokenType.Eof, offset, offset))
}

private fun comment(start: Int, svg: String): Token {
    var offset = start + 2

    if (svg[offset] == '-' && svg[offset + 1] == '-') {
        offset += 2
    } else {
        lexerError(offset, svg)
    }

    var isClosed = false
    while (offset < svg.length - 2) {
        if (svg[offset] == '-' && svg[offset + 1] == '-' && svg[offset + 2] == '>') {
            offset += 3
            isClosed = true
            break
        }
        offset++
    }
    if (!isClosed) {
        lexerError(offset, svg, "Unclosed comment")
    }
    return Token(TokenType.Comment, start, offset)
}

private fun identifier(start: Int, svg: String): Token? {
    var offset = start
    if (svg[offset].isLetter()) {
        offset++
        while (offset < svg.length) {
            if (svg[offset].isLetterOrDigit() || svg[offset] == '-' || svg[offset] == ':' ) {
                offset++
            } else {
                break
            }
        }
        return Token(TokenType.Identifier, start, offset)
    }
    return null
}

private fun lexerError(offset: Int, svg: String, message: String = "Unexpected character"): Nothing {
    error("$message at offset $offset: ${svg.debugString(offset)}")
}
