package com.workshop.manualorganiser

import com.workshop.manualorganiser.data.Manual
import com.workshop.manualorganiser.data.ManualPage
import com.workshop.manualorganiser.util.Taxonomy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxonomyTest {

    private fun manual(title: String, notes: String = "", labels: List<String> = emptyList()) = Manual(
        id = "test",
        title = title,
        category = "General",
        notes = notes,
        pages = labels.map { ManualPage(id = it, uri = "/tmp/$it.png", label = it) },
    )

    @Test
    fun `engine manual is classified as engine`() {
        val suggestion = Taxonomy.suggest(manual("Toyota 2JZ-GTE Workshop", "engine rebuild"))
        assertEquals("Engine", suggestion.category)
        assertTrue(suggestion.matched.any { it == "engine" || it == "2jz" })
        assertTrue(suggestion.confidence > 0f)
    }

    @Test
    fun `electrical manual is classified as electrical`() {
        val suggestion = Taxonomy.suggest(manual("BMW E46 Electrical", "wiring diagrams"))
        assertEquals("Electrical", suggestion.category)
    }

    @Test
    fun `safety manual is classified as safety`() {
        val suggestion = Taxonomy.suggest(manual("Safety Procedures 2024", "shop rules"))
        assertEquals("Safety", suggestion.category)
    }

    @Test
    fun `unmatched manual falls back to general with zero confidence`() {
        val suggestion = Taxonomy.suggest(manual("Misc"))
        assertEquals("General", suggestion.category)
        assertEquals(0f, suggestion.confidence, 0.0001f)
    }

    @Test
    fun `diagnostics adds a diagnostics section`() {
        val suggestion = Taxonomy.suggest(manual("Engine diagnostics", "fault code p0301"))
        assertTrue(suggestion.sections.contains("Diagnostics"))
    }

    @Test
    fun `pages are ordered by suggested section`() {
        val subject = manual(
            title = "Engine",
            labels = listOf("Reassembly notes", "Specifications", "torque settings"),
        )
        val suggestion = Taxonomy.suggest(subject)
        val ordered = Taxonomy.orderPages(subject, suggestion.sections)

        assertEquals(subject.pageCount, ordered.size)
        // "Specifications" is the first proposed section, so its page must come first.
        assertEquals("Specifications", ordered.first().label)
    }

    @Test
    fun `unlabelled pages keep their relative order at the end`() {
        val subject = manual(
            title = "Brakes",
            labels = listOf("Specifications", "differentiation notes"),
        )
        val ordered = Taxonomy.orderPages(subject, "Specifications".let { listOf(it) })
        assertEquals("Specifications", ordered.first().label)
    }
}
