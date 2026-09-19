package com.workshop.manualorganiser.util

/**
 * Offline technical knowledge base backing "AI Technical Help".
 *
 * Every article is real diagnostic content with symptom → cause → test ordering.
 * Search runs locally, so help is available with no signal in a workshop.
 */
object KnowledgeBase {

    data class Article(
        val id: String,
        val code: String,
        val title: String,
        val system: String,
        val severity: String,
        val symptoms: List<String>,
        val causes: List<String>,
        val checks: List<String>,
        val keywords: List<String>,
    )

    val articles: List<Article> = listOf(
        Article(
            "p0301", "P0300/P0301", "Misfire detected (cylinder 1)",
            "Engine", "High",
            listOf("Rough idle", "Flashing check-engine lamp under load", "Loss of power", "Shaking at idle"),
            listOf("Worn spark plug or coil", "Failing injector", "Low compression", "Vacuum leak at that runner", "Valve clearance"),
            listOf(
                "Swap the coil and plug to another cylinder — if the misfire follows, the part is at fault.",
                "Measure injector resistance against the specification for your engine code.",
                "Run a relative compression test before removing anything.",
                "Smoke-test the intake for a leak at that cylinder's runner.",
            ),
            listOf("misfire", "p0300", "p0301", "rough", "shaking", "flashing cel"),
        ),
        Article(
            "p0420", "P0420", "Catalyst efficiency below threshold (Bank 1)",
            "Emissions", "Medium",
            listOf("Check-engine lamp", "Failed emissions test", "No driveability complaint"),
            listOf("Failed catalytic converter", "Upstream/downstream O2 sensor ageing", "Exhaust leak before the catalyst", "Rich running damaging the substrate"),
            listOf(
                "Confirm there is no exhaust leak upstream of the catalyst first.",
                "Compare front and rear O2 sensor switching rates on live data.",
                "Check fuel trims — a rich or misfiring engine will kill a new converter too.",
                "Only replace the catalyst once fuel control and ignition are proven good.",
            ),
            listOf("p0420", "catalyst", "cat", "emissions", "lambda"),
        ),
        Article(
            "p0171", "P0171/P0172", "Fuel system too lean / too rich (Bank 1)",
            "Fuel", "Medium",
            listOf("Check-engine lamp", "Hesitation", "Poor idle", "Black smoke (rich)"),
            listOf("Vacuum or intake leak", "Dirty or failing MAF", "Weak fuel pump or blocked filter", "Leaking injector (rich)", "Exhaust leak (lean)"),
            listOf(
                "Read long- and short-term fuel trims at idle and at 2500 rpm.",
                "Lean at idle only points to a vacuum leak; lean everywhere points to fuel delivery.",
                "Clean the MAF with dedicated cleaner and re-read trims.",
                "Check fuel pressure against specification under load.",
            ),
            listOf("p0171", "p0172", "lean", "rich", "fuel trim", "maf", "vacuum leak"),
        ),
        Article(
            "p0128", "P0128", "Coolant thermostat below regulating temperature",
            "Cooling", "Low",
            listOf("Check-engine lamp", "Slow warm-up", "Weak heater output", "Temperature gauge never reaches normal"),
            listOf("Stuck-open thermostat", "Failed coolant temperature sensor", "Low coolant level"),
            listOf(
                "Compare coolant temperature sensor reading against an infrared thermometer on the housing.",
                "Watch time-to-warm-up on live data — a working engine reaches operating temperature in 5–10 minutes.",
                "Replace the thermostat with the housing and seal as a set.",
            ),
            listOf("p0128", "thermostat", "coolant", "overheating", "warm up", "heater"),
        ),
        Article(
            "p0442", "P0442/P0455", "EVAP system small / gross leak",
            "Evap", "Low",
            listOf("Check-engine lamp", "Fuel smell after refuelling", "Hard to fill the tank"),
            listOf("Loose or failed fuel cap", "Perished filler neck seal", "Rusted or split vapour line", "Failed purge or vent valve"),
            listOf(
                "Start with the cheapest item: the fuel cap seal and its seating.",
                "Smoke-test the EVAP system — it finds a leak in minutes.",
                "Command the purge and vent valves with a scan tool and listen for operation.",
            ),
            listOf("p0442", "p0455", "evap", "fuel cap", "purge valve"),
        ),
        Article(
            "p0562", "P0562/P0563", "System voltage low / high",
            "Electrical", "High",
            listOf("Dim lights at idle", "Multiple unrelated fault codes", "Hard starting", "Warning lamps"),
            listOf("Failing alternator or regulator", "Poor battery terminals", "Corroded engine earth strap", "Battery at end of life"),
            listOf(
                "Measure battery voltage at rest, at idle and under load before anything else.",
                "Check voltage drop across the earth strap while cranking (under 0.2 V).",
                "Re-test after clearing codes — low voltage often produces a cascade of false codes.",
            ),
            listOf("p0562", "p0563", "voltage", "alternator", "charging", "battery"),
        ),
        Article(
            "p0700", "P0700", "Transmission control system fault",
            "Transmission", "High",
            listOf("Check-engine lamp", "Harsh or delayed shifts", "Limp mode", "No upshift"),
            listOf("Transmission control module fault codes", "Low or burnt fluid", "Solenoid pack failure", "Speed sensor fault"),
            listOf(
                "Read the transmission module specifically — P0700 only says 'there are codes in the TCM'.",
                "Check fluid level, condition and smell before electrical diagnosis.",
                "A single unresolved speed-sensor code can cause limp mode.",
            ),
            listOf("p0700", "transmission", "gearbox", "limp", "shift"),
        ),
        Article(
            "u0100", "U0100/U0101", "Lost communication with ECM/PCM",
            "Network", "High",
            listOf("No start", "Multiple warning lamps", "No communication with scan tool", "Instrument cluster dead"),
            listOf("CAN bus wiring break or short", "Blown fuse on the module feed", "Water ingress at a connector", "Failed ECU"),
            listOf(
                "Measure CAN High and CAN Low at the DLC with the ignition on — both should sit near 2.5 V (sum ≈ 5 V).",
                "Check the module's power and ground before suspecting the module itself.",
                "Look for a shorted node by disconnecting modules one at a time.",
            ),
            listOf("u0100", "u0101", "can bus", "communication", "no start", "network"),
        ),
        Article(
            "p0011", "P0011/P0016", "Camshaft timing over-advanced / retarded",
            "Engine", "Medium",
            listOf("Rough idle", "Reduced power", "Poor fuel economy", "Rattle on cold start"),
            listOf("Sludge blocking the oil control valve", "Worn timing chain or tensioner", "Failed variable valve timing solenoid", "Low oil pressure"),
            listOf(
                "Check oil condition and level first — VVT faults are frequently oil-related.",
                "Command the oil control valve with a scan tool and confirm cam advance changes.",
                "Inspect the timing chain for stretch if the phaser does not respond.",
            ),
            listOf("p0011", "vvt", "cam", "timing", "rattle"),
        ),
        Article(
            "no-crank", "-", "Engine does not crank",
            "Starting", "High",
            listOf("Nothing on the key", "Single click", "Rapid clicking"),
            listOf("Flat or failed battery", "Poor terminal or earth connection", "Faulty starter solenoid", "Immobiliser active", "Clutch or park switch"),
            listOf(
                "Headlamps bright and no crank: suspect the immobiliser or starter circuit.",
                "Single loud click: measure battery voltage while cranking — below 9.6 V is a battery or cable fault.",
                "Confirm 12 V on the solenoid trigger wire in the start position.",
            ),
            listOf("no crank", "no start", "click", "starter", "immobiliser", "won't start"),
        ),
        Article(
            "battery-drain", "-", "Battery drains overnight",
            "Electrical", "Medium",
            listOf("Slow crank first thing in the morning", "Battery flat after standing", "New battery dies too"),
            listOf("Parasitic draw above 50 mA", "Failed diode in the alternator", "Body module not sleeping", "Boot or glovebox lamp staying on"),
            listOf(
                "Measure parasitic draw with the vehicle locked and asleep (allow up to 30 minutes).",
                "Anything over 50 mA needs investigating.",
                "Pull fuses one at a time to isolate the circuit, then re-time the sleep cycle.",
            ),
            listOf("drain", "flat battery", "parasitic", "battery", "won't start cold"),
        ),
        Article(
            "rough-idle", "-", "Rough idle",
            "Engine", "Medium",
            listOf("Vibration at idle", "Idle hunts up and down", "Stalling at junctions"),
            listOf("Vacuum leak", "Dirty throttle body or MAF", "Failing ignition component", "Carbon build-up on intake valves (direct injection)"),
            listOf(
                "Check fuel trims at idle; positive trim suggests an unmetered air leak.",
                "Clean the throttle body and re-run the idle relearn procedure.",
                "On direct-injection engines, carbon on the intake valves is a common cause.",
            ),
            listOf("rough idle", "idle", "stall", "hunting", "vibration"),
        ),
        Article(
            "overs-h-eat", "-", "Overheating",
            "Cooling", "High",
            listOf("Temperature gauge in the red", "Steam from the bonnet", "Coolant loss", "Heater blowing cold"),
            listOf("Low coolant or air lock", "Failed thermostat", "Inoperative cooling fan", "Head gasket failure", "Failed water pump"),
            listOf(
                "STOP the engine to avoid warping the head before investigating.",
                "Cold heater with a hot engine is a strong water-pump or air-lock clue.",
                "Test the fan by bridging its switch or commanding it on with a scan tool.",
                "Pressure-test the system and check for combustion gases in the coolant.",
            ),
            listOf("overheating", "overheat", "temperature", "coolant", "steam", "fan"),
        ),
        Article(
            "brake-squeal", "-", "Brake noise or vibration",
            "Brakes", "Medium",
            listOf("Squeal when braking", "Grinding", "Pulsation through the pedal"),
            listOf("Worn pads to the wear indicator", "Glazed or contaminated pads", "Warped or unevenly worn discs", "Missing anti-rattle shims"),
            listOf(
                "Measure pad and disc thickness against the specification before replacing.",
                "Disc run-out over 0.05 mm causes pedal pulsation.",
                "Clean and lubricate the caliper slides with brake-safe grease — never copper slip on the seals.",
            ),
            listOf("brake", "squeal", "grind", "pulsation", "judder", "pads", "discs"),
        ),
        Article(
            "abs-light", "-", "ABS warning lamp on",
            "Brakes", "Medium",
            listOf("ABS lamp illuminated", "Traction control lamp", "ABS inactive under braking"),
            listOf("Wheel speed sensor fault", "Damaged reluctor ring", "Harness break at the strut", "ABS module or pump fault"),
            listOf(
                "Read the ABS module codes — do not clear them first.",
                "Compare all four wheel speeds on live data while rolling at walking pace.",
                "Inspect the harness where it flexes near the strut; breaks are common there.",
            ),
            listOf("abs", "wheel speed", "traction", "warning light"),
        ),
        Article(
            "ac-not-cold", "-", "Air conditioning not cooling",
            "HVAC", "Low",
            listOf("Warm air from the vents", "Compressor not engaging", "Weak cooling at low speed"),
            listOf("Low refrigerant charge", "Leaking condenser or seals", "Failed pressure switch", "Compressor clutch or control valve"),
            listOf(
                "Check static and running pressures before adding refrigerant.",
                "A condenser that never gets hot points to a charge or compressor fault.",
                "Never open the system without recovering the refrigerant first.",
            ),
            listOf("aircon", "a/c", "ac", "cooling", "compressor", "refrigerant"),
        ),
    )

    /** Ranks articles against a free-text query. */
    fun search(query: String): List<Article> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        val tokens = q.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }

        return articles.map { article ->
            var score = 0
            if (article.code.lowercase().split('/').any { it.trim() == q || q.contains(it.trim()) }) score += 6
            article.keywords.forEach { keyword ->
                if (q.contains(keyword)) score += 4
                tokens.forEach { token -> if (keyword.contains(token)) score += 2 }
            }
            if (article.title.lowercase().contains(q)) score += 5
            tokens.forEach { token ->
                if (article.title.lowercase().contains(token)) score += 2
                if (article.symptoms.any { it.lowercase().contains(token) }) score += 1
            }
            article to score
        }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun byId(id: String): Article? = articles.firstOrNull { it.id == id }
}
