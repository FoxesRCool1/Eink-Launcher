package io.github.foxesrcool1.margin.core.routine

import io.github.foxesrcool1.margin.core.json.Json
import io.github.foxesrcool1.margin.core.json.JsonArray
import io.github.foxesrcool1.margin.core.json.JsonObject
import io.github.foxesrcool1.margin.core.json.jsonArrayOf
import io.github.foxesrcool1.margin.core.json.jsonOf
import java.util.Locale

/** Reads and writes `habits/routine.json`. Meant to be editable by hand. */
object RoutineFile {

    const val VERSION = 1

    fun serialise(document: RoutineDocument): String = Json.write(
        JsonObject.of(
            "version" to jsonOf(VERSION),
            "items" to jsonArrayOf(
                document.items.map { item ->
                    JsonObject.of(
                        "id" to jsonOf(item.id),
                        "label" to jsonOf(item.label),
                        "target" to jsonOf(item.target),
                    )
                },
            ),
        ),
    )

    /** Null when the text is not this file. A broken item is dropped. */
    fun parse(text: String): RoutineDocument? {
        val root = Json.parseOrNull(text) as? JsonObject ?: return null
        if (root.int("version") == null) return null

        val items = (root["items"] as? JsonArray)
            ?.objects()
            ?.mapNotNull { entry ->
                val id = entry.string("id")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val label = entry.string("label")?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                RoutineItem(id = id, label = label, target = entry.string("target").orEmpty())
            }
            ?.distinctBy { it.id }
            ?: emptyList()

        return RoutineDocument(items)
    }

    fun idFor(label: String, existing: Set<String>): String {
        val base = label
            .lowercase(Locale.ROOT)
            .map { if (it.isLetterOrDigit()) it else '-' }
            .joinToString("")
            .replace(Regex("-{2,}"), "-")
            .trim('-')
            .take(40)
            .ifBlank { "item" }

        if (base !in existing) return base
        var counter = 2
        while ("$base-$counter" in existing) counter++
        return "$base-$counter"
    }
}
