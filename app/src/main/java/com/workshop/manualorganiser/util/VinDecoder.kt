package com.workshop.manualorganiser.util

/**
 * Offline VIN (Vehicle Identification Number) decoder.
 *
 * Implements the ISO 3779 / 3780 structure:
 *   characters 1-3  WMI (World Manufacturer Identifier)
 *   characters 4-8  VDS (Vehicle Descriptor Section)
 *   character  9    check digit (North-American VINs)
 *   character 10    model year
 *   character 11    plant code
 *   characters 12-17 serial number
 *
 * There is no network access here: every value comes from the standard's own
 * tables, so the decoder works with the radio off.
 */
object VinDecoder {

    private const val LENGTH = 17
    private val ALLOWED = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789"

    /** Transliteration table used for the check-digit algorithm. */
    private val TRANSLITERATION = mapOf(
        'A' to 1, 'B' to 2, 'C' to 3, 'D' to 4, 'E' to 5, 'F' to 6, 'G' to 7,
        'H' to 8, 'J' to 1, 'K' to 2, 'L' to 3, 'M' to 4, 'N' to 5, 'P' to 7,
        'R' to 9, 'S' to 2, 'T' to 3, 'U' to 4, 'V' to 5, 'W' to 6, 'X' to 7,
        'Y' to 8, 'Z' to 9,
    )

    private val WEIGHTS = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

    private val YEAR_CODES = mapOf(
        'A' to 2010, 'B' to 2011, 'C' to 2012, 'D' to 2013, 'E' to 2014, 'F' to 2015,
        'G' to 2016, 'H' to 2017, 'J' to 2018, 'K' to 2019, 'L' to 2020, 'M' to 2021,
        'N' to 2022, 'P' to 2023, 'R' to 2024, 'S' to 2025, 'T' to 2026, 'V' to 2027,
        'W' to 2028, 'X' to 2029, 'Y' to 2030,
        '1' to 2031, '2' to 2032, '3' to 2033, '4' to 2034, '5' to 2035, '6' to 2036,
        '7' to 2037, '8' to 2038, '9' to 2039,
    )

    /** Region by first character (ISO 3780 country block). */
    private val REGION_BY_FIRST = mapOf(
        '1' to "North America", '4' to "North America", '5' to "North America",
        '2' to "North America", '3' to "North America",
        '6' to "Oceania", '7' to "Oceania",
        '8' to "South America", '9' to "South America",
        'A' to "Africa", 'B' to "Africa", 'C' to "Africa", 'D' to "Africa",
        'E' to "Africa", 'F' to "Africa", 'G' to "Africa", 'H' to "Africa",
        'J' to "Asia", 'K' to "Asia", 'L' to "Asia", 'M' to "Asia",
        'N' to "Asia", 'P' to "Asia", 'R' to "Asia",
        'S' to "Europe", 'T' to "Europe", 'U' to "Europe", 'V' to "Europe",
        'W' to "Europe", 'X' to "Europe", 'Y' to "Europe", 'Z' to "Europe",
    )

    /** Country by the first two characters. */
    private val COUNTRY_BY_PREFIX = mapOf(
        "1G" to "United States", "1H" to "United States", "1F" to "United States",
        "1C" to "United States", "1D" to "United States", "1J" to "United States",
        "1N" to "United States", "1V" to "United States", "1T" to "United States",
        "1B" to "United States", "1M" to "United States", "1Y" to "United States",
        "4F" to "United States", "4M" to "United States", "4S" to "United States",
        "4T" to "United States", "4J" to "United States", "4U" to "United States",
        "5F" to "United States", "5N" to "United States", "5T" to "United States",
        "5Y" to "United States", "5U" to "United States",
        "2C" to "Canada", "2F" to "Canada", "2G" to "Canada", "2H" to "Canada",
        "2T" to "Canada", "2M" to "Canada",
        "3F" to "Mexico", "3G" to "Mexico", "3H" to "Mexico", "3N" to "Mexico",
        "3V" to "Mexico",
        "JA" to "Japan", "JF" to "Japan", "JH" to "Japan", "JM" to "Japan",
        "JN" to "Japan", "JS" to "Japan", "JT" to "Japan", "JY" to "Japan",
        "KL" to "South Korea", "KM" to "South Korea", "KN" to "South Korea",
        "KP" to "South Korea",
        "LS" to "China", "LV" to "China", "LF" to "China", "LJ" to "China",
        "LR" to "China", "L6" to "China",
        "SA" to "United Kingdom", "SB" to "United Kingdom", "SC" to "United Kingdom",
        "SH" to "United Kingdom", "SJ" to "United Kingdom",
        "VF" to "France", "VH" to "France", "VG" to "France",
        "VS" to "Spain", "VR" to "Spain",
        "VW" to "Germany", "WA" to "Germany", "WB" to "Germany", "WD" to "Germany",
        "WF" to "Germany", "WG" to "Germany", "WP" to "Germany", "WV" to "Germany",
        "WH" to "Germany", "WU" to "Germany",
        "YK" to "Sweden", "YL" to "Sweden", "YV" to "Sweden", "YS" to "Sweden",
        "YU" to "Sweden",
        "YH" to "Finland", "YM" to "Finland",
        "ZA" to "Italy", "ZB" to "Italy", "ZD" to "Italy", "ZF" to "Italy",
        "ZH" to "Italy", "ZM" to "Italy",
    )

    /** Manufacturer by WMI (first three characters). Curated from Iso 3779 WMIs. */
    private val MANUFACTURER_BY_WMI = mapOf(
        "1G1" to "Chevrolet", "1GC" to "Chevrolet", "1GN" to "Chevrolet",
        "1G4" to "Buick", "1G6" to "Cadillac", "1GT" to "GMC",
        "1FA" to "Ford", "1FB" to "Ford", "1FC" to "Ford", "1FD" to "Ford",
        "1FM" to "Ford", "1FT" to "Ford",
        "1HG" to "Honda", "2HG" to "Honda", "3HG" to "Honda", "5FN" to "Honda",
        "1C3" to "Chrysler", "1C4" to "Jeep", "1C6" to "Ram", "1B3" to "Dodge",
        "1J4" to "Jeep", "2C3" to "Chrysler", "2C4" to "Chrysler",
        "1D3" to "Dodge", "1D7" to "Dodge",
        "4T1" to "Toyota", "4T3" to "Toyota", "5TD" to "Toyota", "5TF" to "Toyota",
        "2T1" to "Toyota", "2T3" to "Toyota", "3T1" to "Toyota",
        "4S3" to "Subaru", "4S4" to "Subaru", "JF1" to "Subaru", "JF2" to "Subaru",
        "5YJ" to "Tesla", "7SA" to "Tesla", "7G2" to "Tesla",
        "5NP" to "Hyundai", "5NM" to "Hyundai", "5N1" to "Hyundai",
        "5XY" to "Kia", "5XX" to "Kia", "KND" to "Kia",
        "1N4" to "Nissan", "1N6" to "Nissan", "5N1X" to "Nissan", "JN1" to "Nissan",
        "MA3" to "Suzuki", "JS2" to "Suzuki", "JSA" to "Suzuki",
        "JHM" to "Honda", "JHL" to "Honda", "JTD" to "Toyota", "JTH" to "Lexus",
        "JTJ" to "Lexus", "JTB" to "Toyota", "JH4" to "Acura", "JH2" to "Honda",
        "JM1" to "Mazda", "JM3" to "Mazda", "JMZ" to "Mazda",
        "JMB" to "Mitsubishi", "JA4" to "Mitsubishi", "4A3" to "Mitsubishi",
        "WBA" to "BMW", "WBS" to "BMW M", "WBY" to "BMW",
        "WDB" to "Mercedes-Benz", "WDD" to "Mercedes-Benz", "WDC" to "Mercedes-Benz",
        "W1K" to "Mercedes-Benz", "4JG" to "Mercedes-Benz",
        "WVW" to "Volkswagen", "WV1" to "Volkswagen Commercial", "WV2" to "Volkswagen",
        "3VW" to "Volkswagen", "WVG" to "Volkswagen",
        "WA1" to "Audi", "WAU" to "Audi", "TRU" to "Audi", "WUA" to "Audi",
        "WP0" to "Porsche", "WP1" to "Porsche",
        "YV1" to "Volvo", "YV4" to "Volvo", "LVY" to "Volvo", "YV2" to "Volvo",
        "SAJ" to "Jaguar", "SAD" to "Jaguar", "SAL" to "Land Rover",
        "SCC" to "Lotus", "SCF" to "Aston Martin", "SCA" to "Rolls-Royce",
        "VF1" to "Renault", "VF2" to "Renault", "VF3" to "Peugeot",
        "VF7" to "Citroën", "VR1" to "Citroën",
        "ZAM" to "Maserati", "ZAR" to "Alfa Romeo", "ZFA" to "Fiat",
        "ZFF" to "Ferrari", "ZHW" to "Lamborghini", "Z9" to "Bugatti",
        "TMB" to "Škoda", "TMA" to "Hyundai", "TRA" to "Iveco",
        "KNA" to "Kia", "KMH" to "Hyundai", "KMT" to "Genesis",
        "LSV" to "SAIC Volkswagen", "LVS" to "Changan Ford", "LGB" to "Dongfeng",
        "9BW" to "Volkswagen do Brasil", "93H" to "Honda", "9BD" to "Fiat",
        "8AP" to "Fiat Argentina", "8AF" to "Ford Argentina",
    )

    /** Well-known example VINs offered on the decoder screen. */
    val EXAMPLES: List<String> = listOf(
        "1HGCM82633A004352", // Honda Accord (US)
        "5YJ3E1EA7HF000337", // Tesla Model 3
        "WBA8E9G50GNT12345", // BMW 3 Series
        "JHMCM56557C404453", // Honda (Japan)
        "1FTFW1ET5DFC10312", // Ford F-150
    )

    data class Result(
        val vin: String,
        val valid: Boolean,
        val region: String,
        val country: String,
        val manufacturer: String,
        val modelYear: Int?,
        val modelYearCode: Char?,
        val plantCode: String,
        val serial: String,
        val checkDigit: Char,
        val expectedCheckDigit: Char?,
        val checksumStatus: Checksum,
        val errors: List<VinError>,
        val notes: List<String>,
    )

    enum class Checksum { VALID, INVALID, NOT_APPLICABLE }

    enum class VinError { WRONG_LENGTH, ILLEGAL_CHARACTER }

    fun normalize(raw: String): String =
        raw.trim().uppercase().filter { it.isLetterOrDigit() }

    /** Returns the errors found in [vin] (empty means structurally valid). */
    fun validate(vin: String): List<VinError> = buildList {
        if (vin.length != LENGTH) add(VinError.WRONG_LENGTH)
        if (vin.any { it !in ALLOWED }) add(VinError.ILLEGAL_CHARACTER)
    }

    /** Whether the VIN is North American (check digit applies). */
    fun isNorthAmerican(vin: String): Boolean =
        vin.isNotEmpty() && vin[0] in charArrayOf('1', '2', '3', '4', '5')

    /** Computes the ISO 3779 check digit for [vin], or null if not computable. */
    fun computeCheckDigit(vin: String): Char? {
        if (vin.length != LENGTH) return null
        var sum = 0
        vin.forEachIndexed { index, char ->
            val value = when {
                char.isDigit() -> char - '0'
                else -> TRANSLITERATION[char] ?: return null
            }
            sum += value * WEIGHTS[index]
        }
        val remainder = sum % 11
        return if (remainder == 10) 'X' else remainder.digitToChar()
    }

    fun decode(raw: String): Result {
        val vin = normalize(raw)
        val errors = validate(vin).toMutableList()
        val notes = mutableListOf<String>()

        if (vin.isEmpty()) {
            return Result(
                vin = vin, valid = false, region = "", country = "", manufacturer = "",
                modelYear = null, modelYearCode = null, plantCode = "", serial = "",
                checkDigit = ' ', expectedCheckDigit = null,
                checksumStatus = Checksum.NOT_APPLICABLE, errors = errors, notes = notes,
            )
        }

        val first = vin.firstOrNull() ?: ' '
        val region = REGION_BY_FIRST[first] ?: "Unknown"
        val prefix = vin.take(2)
        val country = COUNTRY_BY_PREFIX[prefix] ?: region

        val manufacturer = MANUFACTURER_BY_WMI[vin.take(3)]
            ?: MANUFACTURER_BY_WMI[vin.take(4)] // a few WMIs are 4 characters
            ?: "Unknown"

        val yearCode = vin.getOrNull(9)
        val modelYear = yearCode?.let { YEAR_CODES[it] }?.let { base ->
            // The code table repeats every 30 years; pick the cycle nearest to now.
            val candidate = base
            val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            if (candidate > thisYear + 1) candidate - 30 else candidate
        }
        if (yearCode != null && YEAR_CODES.containsKey(yearCode)) {
            notes.add("Model year is decoded from position 10; the code table repeats every 30 years.")
        }

        val plantCode = vin.getOrNull(10)?.toString() ?: ""
        val serial = if (vin.length >= 17) vin.substring(11, 17) else vin.takeLast(6)

        val expected = computeCheckDigit(vin)
        val actual = vin.getOrNull(8) ?: ' '
        val checksumStatus = when {
            !isNorthAmerican(vin) -> Checksum.NOT_APPLICABLE
            expected == null -> Checksum.NOT_APPLICABLE
            expected == actual -> Checksum.VALID
            else -> Checksum.INVALID
        }
        if (checksumStatus == Checksum.NOT_APPLICABLE && expected != null) {
            notes.add("Check-digit validation applies to North-American VINs only.")
        }
        if (checksumStatus == Checksum.INVALID) {
            notes.add("The 9th character should be '$expected' for this VIN.")
        }
        if (manufacturer == "Unknown" && vin.length >= 3) {
            notes.add("WMI '${vin.take(3)}' is not in the built-in manufacturer table.")
        }

        return Result(
            vin = vin,
            valid = errors.isEmpty(),
            region = region,
            country = country,
            manufacturer = manufacturer,
            modelYear = modelYear,
            modelYearCode = yearCode,
            plantCode = plantCode,
            serial = serial,
            checkDigit = actual,
            expectedCheckDigit = expected,
            checksumStatus = checksumStatus,
            errors = errors,
            notes = notes,
        )
    }
}
