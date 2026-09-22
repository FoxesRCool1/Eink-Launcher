package io.github.foxesrcool1.margin.core.json

/**
 * A JSON value.
 *
 * The plan writes `habits.json` and `annotations/<book-id>.json`, so the app
 * needs to read and write JSON. It does that with this instead of a library,
 * for two reasons:
 *
 * 1. `org.json` is stubbed out in a plain unit test. A test would pass while
 *    the real code did nothing.
 * 2. Every serialisation library for Kotlin comes with a compiler plugin tied
 *    to a Kotlin version, and AGP 9 owns the Kotlin version now. That pairing
 *    has already cost this project two red builds.
 *
 * This holds no Android class, so it is checked by plain JUnit tests.
 */
sealed interface JsonValue

data class JsonObject(val entries: Map<String, JsonValue>) : JsonValue {

    operator fun get(key: String): JsonValue? = entries[key]

    fun string(key: String): String? = (entries[key] as? JsonString)?.value

    fun number(key: String): Double? = (entries[key] as? JsonNumber)?.value

    fun long(key: String): Long? = number(key)?.toLong()

    fun int(key: String): Int? = number(key)?.toInt()

    fun boolean(key: String): Boolean? = (entries[key] as? JsonBoolean)?.value

    fun obj(key: String): JsonObject? = entries[key] as? JsonObject

    fun array(key: String): JsonArray? = entries[key] as? JsonArray

    companion object {
        fun of(vararg pairs: Pair<String, JsonValue>): JsonObject = JsonObject(linkedMapOf(*pairs))
    }
}

data class JsonArray(val items: List<JsonValue>) : JsonValue {
    val size: Int get() = items.size
    operator fun get(index: Int): JsonValue = items[index]
    fun objects(): List<JsonObject> = items.filterIsInstance<JsonObject>()
    fun strings(): List<String> = items.filterIsInstance<JsonString>().map { it.value }
}

data class JsonString(val value: String) : JsonValue

data class JsonNumber(val value: Double) : JsonValue {
    constructor(value: Long) : this(value.toDouble())
    constructor(value: Int) : this(value.toDouble())
}

data class JsonBoolean(val value: Boolean) : JsonValue

data object JsonNull : JsonValue

/** Shorthand for building a value. */
fun jsonOf(value: String): JsonValue = JsonString(value)

fun jsonOf(value: Long): JsonValue = JsonNumber(value)

fun jsonOf(value: Int): JsonValue = JsonNumber(value)

fun jsonOf(value: Boolean): JsonValue = JsonBoolean(value)

fun jsonArrayOf(items: List<JsonValue>): JsonArray = JsonArray(items)
