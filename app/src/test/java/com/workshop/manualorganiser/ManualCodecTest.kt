package com.workshop.manualorganiser

import com.workshop.manualorganiser.data.Manual
import com.workshop.manualorganiser.data.ManualCodec
import com.workshop.manualorganiser.data.ManualPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the durability format directly: whatever the repository writes must
 * read back identically, including every page, or the app loses data on restart.
 */
class ManualCodecTest {

    private val sample = Manual(
        id = "m1",
        title = "Toyota 2JZ-GTE Workshop",
        category = "Engine",
        notes = "Engine rebuild, torque specs",
        vin = "1HGCM82633A004352",
        pages = listOf(
            ManualPage(id = "p1", uri = "/data/files/library/a.png", label = "Specifications"),
            ManualPage(id = "p2", uri = "/data/files/library/b.png", label = "Reassembly"),
        ),
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_500_000L,
    )

    @Test
    fun `library round trips without loss`() {
        val encoded = ManualCodec.encodeLibrary(listOf(sample))
        val decoded = ManualCodec.decodeLibrary(encoded)

        assertEquals(1, decoded.size)
        assertEquals(sample, decoded.first())
    }

    @Test
    fun `every page survives the round trip in order`() {
        val decoded = ManualCodec.decodeLibrary(ManualCodec.encodeLibrary(listOf(sample))).first()
        assertEquals(listOf("Specifications", "Reassembly"), decoded.pages.map { it.label })
        assertEquals(sample.pages.map { it.id }, decoded.pages.map { it.id })
        assertEquals(sample.pages.map { it.uri }, decoded.pages.map { it.uri })
    }

    @Test
    fun `empty library round trips`() {
        val decoded = ManualCodec.decodeLibrary(ManualCodec.encodeLibrary(emptyList()))
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `malformed input yields an empty library instead of throwing`() {
        assertTrue(ManualCodec.decodeLibrary("{ not json").isEmpty())
        assertTrue(ManualCodec.decodeLibrary("").isEmpty())
    }

    @Test
    fun `missing optional fields fall back to defaults`() {
        val decoded = ManualCodec.decodeLibrary("""{"manuals":[{"id":"x","title":"Bare"}]}""")
        assertEquals(1, decoded.size)
        assertEquals("General", decoded.first().category)
        assertTrue(decoded.first().pages.isEmpty())
        assertEquals("", decoded.first().notes)
    }

    @Test
    fun `single manual encoding keeps its identifier`() {
        val json = ManualCodec.encode(sample)
        assertEquals("m1", json.getString("id"))
        assertEquals(2, json.getJSONArray("pages").length())
    }
}
