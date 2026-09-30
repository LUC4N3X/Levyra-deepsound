package com.luc4n3x.levyra.nexus.playlistimport

class BoundedJsonException(message: String) : Exception(message)

object BoundedJson {
    private const val MAX_DEPTH = 64

    fun parse(text: String): Any? {
        if (text.length > MAX_PLAYLIST_IMPORT_TEXT_CHARS) throw BoundedJsonException("JSON too large")
        val reader = Reader(text)
        reader.skipWhitespace()
        val value = reader.readValue(0)
        reader.skipWhitespace()
        if (!reader.atEnd()) throw BoundedJsonException("Trailing content at ${reader.index}")
        return value
    }

    fun parseOrNull(text: String): Any? = try {
        parse(text)
    } catch (_: BoundedJsonException) {
        null
    }

    private class Reader(private val source: String) {
        var index = 0

        fun atEnd(): Boolean = index >= source.length

        fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index++
        }

        fun readValue(depth: Int): Any? {
            if (depth > MAX_DEPTH) throw BoundedJsonException("JSON nested too deeply")
            if (atEnd()) throw BoundedJsonException("Unexpected end of JSON")
            return when (val char = source[index]) {
                '{' -> readObject(depth)
                '[' -> readArray(depth)
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                else -> if (char == '-' || char.isDigit()) readNumber() else {
                    throw BoundedJsonException("Unexpected character at $index")
                }
            }
        }

        private fun readObject(depth: Int): Map<String, Any?> {
            index++
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                index++
                return result
            }
            while (true) {
                skipWhitespace()
                if (peek() != '"') throw BoundedJsonException("Expected key at $index")
                val key = readString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                result[key] = readValue(depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return result
                    }
                    else -> throw BoundedJsonException("Expected , or } at $index")
                }
            }
        }

        private fun readArray(depth: Int): List<Any?> {
            index++
            val result = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                index++
                return result
            }
            while (true) {
                skipWhitespace()
                result += readValue(depth + 1)
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return result
                    }
                    else -> throw BoundedJsonException("Expected , or ] at $index")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val builder = StringBuilder()
            while (true) {
                if (atEnd()) throw BoundedJsonException("Unterminated string")
                val char = source[index++]
                when {
                    char == '"' -> return builder.toString()
                    char == '\\' -> {
                        if (atEnd()) throw BoundedJsonException("Unterminated escape")
                        when (val escaped = source[index++]) {
                            '"', '\\', '/' -> builder.append(escaped)
                            'b' -> builder.append('\b')
                            'f' -> builder.append('\u000C')
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'u' -> {
                                if (index + 4 > source.length) throw BoundedJsonException("Bad unicode escape")
                                val code = source.substring(index, index + 4).toIntOrNull(16)
                                    ?: throw BoundedJsonException("Bad unicode escape")
                                builder.append(code.toChar())
                                index += 4
                            }
                            else -> throw BoundedJsonException("Bad escape at $index")
                        }
                    }
                    else -> builder.append(char)
                }
            }
        }

        private fun readNumber(): Any {
            val start = index
            if (peek() == '-') index++
            while (!atEnd() && (source[index].isDigit() || source[index] in ".eE+-")) index++
            val raw = source.substring(start, index)
            val integral = raw.none { it == '.' || it == 'e' || it == 'E' }
            if (integral) raw.toLongOrNull()?.let { return it }
            return raw.toDoubleOrNull() ?: throw BoundedJsonException("Bad number at $start")
        }

        private fun readLiteral(literal: String, value: Any?): Any? {
            if (!source.startsWith(literal, index)) throw BoundedJsonException("Bad literal at $index")
            index += literal.length
            return value
        }

        private fun peek(): Char = if (atEnd()) '\u0000' else source[index]

        private fun expect(char: Char) {
            if (peek() != char) throw BoundedJsonException("Expected $char at $index")
            index++
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun Any?.jsonObject(): Map<String, Any?>? = this as? Map<String, Any?>

@Suppress("UNCHECKED_CAST")
internal fun Any?.jsonArray(): List<Any?>? = this as? List<Any?>

internal fun Map<String, Any?>.string(vararg keys: String): String {
    for (key in keys) {
        when (val value = this[key]) {
            is String -> if (value.isNotBlank()) return value.trim()
            is Long -> return value.toString()
            is Double -> return value.toLong().toString()
        }
    }
    return ""
}

internal fun Map<String, Any?>.long(vararg keys: String): Long? {
    for (key in keys) {
        when (val value = this[key]) {
            is Long -> return value
            is Double -> return value.toLong()
            is String -> value.trim().toLongOrNull()?.let { return it }
        }
    }
    return null
}

internal fun Map<String, Any?>.bool(vararg keys: String): Boolean? {
    for (key in keys) {
        when (val value = this[key]) {
            is Boolean -> return value
            is Long -> return value != 0L
            is String -> when (value.trim().lowercase()) {
                "true", "1", "yes", "explicit" -> return true
                "false", "0", "no", "clean" -> return false
            }
        }
    }
    return null
}
