package com.workshop.manualorganiser

import com.workshop.manualorganiser.util.VinDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VinDecoderTest {

    @Test
    fun `normalise strips separators and case`() {
        assertEquals("1HGCM82633A004352", VinDecoder.normalize(" 1hgcm8-2633a004352 "))
    }

    @Test
    fun `wrong length is reported`() {
        val errors = VinDecoder.validate("1HGCM82633A")
        assertTrue(errors.contains(VinDecoder.VinError.WRONG_LENGTH))
    }

    @Test
    fun `illegal characters are rejected`() {
        // I, O and Q may never appear in a VIN.
        val errors = VinDecoder.validate("1HGCM8263IA004352")
        assertTrue(errors.contains(VinDecoder.VinError.ILLEGAL_CHARACTER))
    }

    @Test
    fun `check digit algorithm is self consistent`() {
        // Build a VIN whose 9th character is the computed check digit, then verify
        // the decoder accepts it. This tests the algorithm without hard-coding an
        // external lookup table.
        val published = "1HGCM82633A004352"
        val body = published.substring(0, 8) + "X" + published.substring(9)
        val expected = VinDecoder.computeCheckDigit(body)
        assertNotNull(expected)

        val rebuilt = body.substring(0, 8) + expected + body.substring(9)
        // The published VIN must be self-consistent with the algorithm…
        assertEquals(published, rebuilt)
        // …and the decoder must accept it.
        assertEquals(VinDecoder.Checksum.VALID, VinDecoder.decode(rebuilt).checksumStatus)
    }

    @Test
    fun `tampered check digit is caught`() {
        val validVin = "1HGCM82633A004352"
        val computed = VinDecoder.computeCheckDigit(validVin)
        val wrong = if (computed == '0') '1' else '0'
        val tampered = validVin.substring(0, 8) + wrong + validVin.substring(9)
        assertEquals(VinDecoder.Checksum.INVALID, VinDecoder.decode(tampered).checksumStatus)
    }

    @Test
    fun `north american vin decodes country and manufacturer`() {
        val result = VinDecoder.decode("1HGCM82633A004352")
        assertEquals("North America", result.region)
        assertEquals("United States", result.country)
        assertEquals("Honda", result.manufacturer)
        assertEquals(2003, result.modelYear)
        assertEquals("004352", result.serial)
    }

    @Test
    fun `german vin decodes region and manufacturer`() {
        val result = VinDecoder.decode("WBA8E9G50GNT12345")
        assertEquals("Europe", result.region)
        assertEquals("Germany", result.country)
        assertEquals("BMW", result.manufacturer)
        assertEquals(2016, result.modelYear)
    }

    @Test
    fun `check digit does not apply outside north america`() {
        val result = VinDecoder.decode("JHMCM56557C404453")
        assertEquals(VinDecoder.Checksum.NOT_APPLICABLE, result.checksumStatus)
        assertEquals("Japan", result.country)
        assertEquals("Honda", result.manufacturer)
    }

    @Test
    fun `model year table rolls back for older codes`() {
        // 'L' maps to 2020 in the current cycle and must not be reported as 2050.
        val result = VinDecoder.decode("WBA8E9G50LNT12345")
        assertEquals(2020, result.modelYear)
    }

    @Test
    fun `unknown wmi is flagged but still decoded`() {
        val result = VinDecoder.decode("XXXCM82633A004352")
        assertEquals("Unknown", result.manufacturer)
        assertTrue(result.notes.any { it.contains("WMI") })
        assertFalse(result.valid.not() && result.vin.isEmpty())
    }
}
