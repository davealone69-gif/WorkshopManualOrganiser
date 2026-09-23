package com.workshop.manualorganiser.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for the on-disk JSON shape of the library.
 * Both the repository and the export/import feature use this, so the formats
 * can never drift apart.
 */
object ManualCodec {

    const val KEY_MANUALS = "manuals"
    const val KEY_VERSION = "schemaVersion"
    const val SCHEMA_VERSION = 1

    fun encode(manual: Manual): JSONObject = JSONObject().apply {
        put("id", manual.id)
        put("title", manual.title)
        put("category", manual.category)
        put("notes", manual.notes)
        put("vin", manual.vin)
        put("createdAt", manual.createdAt)
        put("updatedAt", manual.updatedAt)
        put("pages", JSONArray().apply {
            manual.pages.forEach { page ->
                put(JSONObject().apply {
                    put("id", page.id)
                    put("uri", page.uri)
                    put("label", page.label)
                    put("kind", page.kind)
                })
            }
        })
    }

    fun decode(json: JSONObject): Manual = Manual(
        id = json.optString("id"),
        title = json.optString("title"),
        category = json.optString("category", "General"),
        notes = json.optString("notes"),
        vin = json.optString("vin"),
        pages = json.optJSONArray("pages")?.let { pages ->
            (0 until pages.length()).mapNotNull { index ->
                pages.optJSONObject(index)?.let { page ->
                    ManualPage(
                        id = page.optString("id"),
                        uri = page.optString("uri"),
                        label = page.optString("label"),
                        kind = page.optString("kind", ManualPage.KIND_IMAGE),
                    )
                }
            }
        } ?: emptyList(),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
    )

    fun encodeLibrary(manuals: List<Manual>): String {
        val root = JSONObject()
        root.put(KEY_VERSION, SCHEMA_VERSION)
        root.put(KEY_MANUALS, JSONArray().apply { manuals.forEach { put(encode(it)) } })
        return root.toString()
    }

    fun decodeLibraryStrict(text: String): List<Manual> {
        val root = JSONObject(text)
        val version = root.optInt(KEY_VERSION, -1)
        require(version in 1..SCHEMA_VERSION) { "Unsupported library schema version: $version" }
        val array = root.optJSONArray(KEY_MANUALS) ?: error("Library has no manuals array")
        return (0 until array.length()).map { index ->
            decode(array.getJSONObject(index))
        }
    }

    fun decodeLibrary(text: String): List<Manual> =
        runCatching { decodeLibraryStrict(text) }.getOrElse { emptyList() }
}
