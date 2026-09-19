package com.workshop.manualorganiser.data

/** A single scanned or imported page belonging to a [Manual]. */
data class ManualPage(
    val id: String,
    /** Absolute file path inside app-private storage. */
    val uri: String,
    val label: String = "",
    val kind: String = KIND_IMAGE,
) {
    companion object {
        const val KIND_IMAGE = "image"
        const val KIND_PDF = "pdf"
    }
}

/** A workshop manual together with its pages. */
data class Manual(
    val id: String,
    val title: String,
    val category: String,
    val notes: String = "",
    val vin: String = "",
    val pages: List<ManualPage> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val pageCount: Int get() = pages.size

    /**
     * Text used by the auto-classifier.
     *
     * The existing [category] is deliberately excluded: including it would make
     * every already-categorised manual match its own category keyword and lock in
     * a wrong classification.
     */
    fun classificationText(): String = buildString {
        append(title).append(' ')
        append(notes).append(' ')
        pages.forEach { append(it.label).append(' ') }
    }
}
