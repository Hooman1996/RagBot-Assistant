package org.ragbot.replica

/** A value at a place in a mock file. A missing or wrong field fails with that place. */
internal class Json(private val path: String, private val value: Any?) {
    private fun fail(expected: String): Nothing = throw MockDataError("$path باید $expected باشد.")

    private fun field(key: String) =
        (value as? Map<*, *> ?: fail("شیء")).let {
            if (key !in it) throw MockDataError("فیلد $path.$key وجود ندارد.")
            Json("$path.$key", it[key])
        }

    operator fun get(key: String) = field(key)

    fun has(key: String) = (value as? Map<*, *>)?.containsKey(key) == true

    val isNull
        get() = value == null

    fun string() = value as? String ?: fail("متن")

    fun long() = value as? Long ?: fail("عدد صحیح")

    fun int() = long().toInt()

    fun bool() = value as? Boolean ?: fail("درست یا نادرست")

    fun <T> list(item: (Json) -> T) =
        (value as? List<*> ?: fail("فهرست")).mapIndexed { i, v -> item(Json("$path[$i]", v)) }

    fun <T> map(item: (Json) -> T) =
        (value as? Map<*, *> ?: fail("شیء")).entries.associate { (k, v) ->
            k as String to item(Json("$path.$k", v))
        }

    fun str(key: String) = field(key).string()

    fun long(key: String) = field(key).long()

    fun int(key: String) = field(key).int()

    fun bool(key: String) = field(key).bool()

    fun strOrNull(key: String) = field(key).let { if (it.isNull) null else it.string() }

    fun longOrNull(key: String) = field(key).let { if (it.isNull) null else it.long() }

    fun strings(key: String) = field(key).list { it.string() }

    fun <T> list(key: String, item: (Json) -> T) = field(key).list(item)

    companion object {
        /** A small strict JSON reader; the app has no JSON library and needs no other parts. */
        fun parse(file: String, text: String): Json {
            val reader = Reader(text)
            val value = reader.value()
            reader.end()
            return Json(file, value)
        }
    }

    private class Reader(val text: String) {
        var at = 0

        fun fail(message: String): Nothing =
            throw MockDataError(
                "خطای JSON در خط ${text.take(at).count { it == '\n' } + 1}: $message"
            )

        fun space() {
            while (at < text.length && text[at].isWhitespace()) at++
        }

        fun end() {
            space()
            if (at != text.length) fail("متن اضافه")
        }

        fun eat(ch: Char): Boolean {
            space()
            return (at < text.length && text[at] == ch).also { if (it) at++ }
        }

        fun need(ch: Char) {
            if (!eat(ch)) fail("نویسهٔ $ch لازم است")
        }

        fun value(): Any? {
            space()
            return when {
                eat('{') -> entries('}') { key -> key to value() }.toMap()
                eat('[') -> entries(']') { value() }
                at < text.length && text[at] == '"' -> string()
                text.startsWith("true", at) -> true.also { at += 4 }
                text.startsWith("false", at) -> false.also { at += 5 }
                text.startsWith("null", at) -> null.also { at += 4 }
                else -> number()
            }
        }

        fun <T> entries(close: Char, item: (String) -> T): List<T> {
            val items = mutableListOf<T>()
            if (eat(close)) return items
            do {
                space()
                val key =
                    if (close == '}') string().also { need(':') } else ""
                items += item(key)
            } while (eat(','))
            need(close)
            return items
        }

        fun string(): String {
            need('"')
            val out = StringBuilder()
            while (true) {
                if (at >= text.length) fail("متن بسته نشده")
                val ch = text[at++]
                if (ch == '"') return out.toString()
                if (ch != '\\') {
                    out.append(ch)
                    continue
                }
                when (val escaped = text.getOrNull(at++)) {
                    'n' -> out.append('\n')
                    't' -> out.append('\t')
                    '"', '\\', '/' -> out.append(escaped)
                    'u' -> {
                        out.append(text.substring(at, at + 4).toInt(16).toChar())
                        at += 4
                    }
                    else -> fail("نویسهٔ گریز نامعتبر")
                }
            }
        }

        // Money is exact: a value with a fraction or exponent is rejected, never rounded.
        fun number(): Long {
            val start = at
            if (at < text.length && text[at] == '-') at++
            while (at < text.length && text[at] in '0'..'9') at++
            return text.substring(start, at).toLongOrNull() ?: fail("مقدار نامعتبر")
        }
    }
}
