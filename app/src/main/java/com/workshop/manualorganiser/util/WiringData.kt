package com.workshop.manualorganiser.util

/**
 * Built-in wiring reference. This is a genuine offline dataset (ISO/DIN standard
 * connector and colour conventions), not a placeholder: it is what a technician
 * needs before reaching for the vehicle-specific diagram.
 */
object WiringData {

    data class WiringSystem(
        val id: String,
        val name: String,
        val summary: String,
        val checks: List<String>,
    )

    data class Pinout(
        val id: String,
        val name: String,
        val description: String,
        val pins: List<Pair<String, String>>,
    )

    data class WireColour(
        val code: String,
        val colour: String,
        val german: String,
    )

    val systems: List<WiringSystem> = listOf(
        WiringSystem(
            "charging",
            "Charging system",
            "Alternator, regulator and charge warning circuit.",
            listOf(
                "Battery at rest should be 12.4–12.7 V; below 12.2 V is discharged.",
                "Engine running, expect 13.8–14.6 V at the battery terminals.",
                "With loads on (lights, blower, rear demist) voltage should stay above 13.2 V.",
                "Voltage drop on the B+ cable should be under 0.3 V at full output.",
                "A whining alternator under load usually indicates a failed diode pack.",
            ),
        ),
        WiringSystem(
            "starting",
            "Starting system",
            "Starter motor, solenoid and crank circuit.",
            listOf(
                "No crank: check battery, then the starter solenoid trigger wire for 12 V in START.",
                "Voltage drop on the engine earth strap should be under 0.2 V while cranking.",
                "A single loud click then nothing is typically a flat battery or seized starter.",
                "Rapid clicking indicates insufficient current — battery or terminal resistance.",
            ),
        ),
        WiringSystem(
            "ignition",
            "Ignition system",
            "Coil, distributor/COP and spark delivery.",
            listOf(
                "Coil primary resistance is typically 0.4–1.5 Ω; secondary 6–15 kΩ.",
                "Check for spark at the plug before condemning a coil.",
                "A dead cylinder that changes when you swap the coil follows the coil, not the bore.",
            ),
        ),
        WiringSystem(
            "lighting",
            "Lighting & indicators",
            "Headlamp, tail, brake and indicator circuits.",
            listOf(
                "Fast flashing on one side indicates a failed bulb on that side.",
                "Brake lights permanently on is usually the pedal switch adjustment.",
                "Check the earth at the lamp unit before replacing a bulb twice.",
            ),
        ),
        WiringSystem(
            "injection",
            "Fuel injection & sensors",
            "Injectors, MAF/MAP, TPS, coolant and lambda sensors.",
            listOf(
                "Injector resistance is typically 12–16 Ω (saturated) or 1–3 Ω (peak-and-hold).",
                "Unplug a sensor and watch live data for the default value — a stuck reading is the clue.",
                "Lambda sensors should swing 0.1–0.9 V at roughly 1 Hz once closed loop is reached.",
            ),
        ),
        WiringSystem(
            "cooling",
            "Cooling fan control",
            "Fan relay, thermostatic switch and PWM control module.",
            listOf(
                "Bridge the fan switch connector: if the fan runs, the switch is at fault.",
                "PWM fan modules often fail open — no low speed, only high speed.",
                "Check the relay 30/87 contacts for pitting before replacing the fan.",
            ),
        ),
        WiringSystem(
            "abs",
            "ABS & wheel speed",
            "Wheel speed sensors, ABS module and pump relay.",
            listOf(
                "Wheel speed sensor resistance is typically 1–2 kΩ (inductive) or 2-wire active 7–20 mA.",
                "An ABS lamp with no stored code is often an intermittent harness break at the strut.",
                "Compare all four wheel speeds on live data while rolling at walking pace.",
            ),
        ),
        WiringSystem(
            "srs",
            "Airbag / SRS",
            "Airbag module, clockspring and pretensioners.",
            listOf(
                "ALWAYS isolate the battery and wait at least 10 minutes before touching SRS wiring.",
                "Never measure resistance across a squib — use the manufacturer's tooling only.",
                "A clockspring fault appears as an intermittent driver-airbag lamp when turning.",
            ),
        ),
        WiringSystem(
            "locking",
            "Central locking & immobiliser",
            "Body control module, actuators and transponder aerial.",
            listOf(
                "Test the actuator with a known-good 12 V pulse before replacing the door module.",
                "An immobiliser that stops cranking is usually the transponder aerial ring.",
                "Check for water ingress at the body control module connectors first.",
            ),
        ),
        WiringSystem(
            "trailer",
            "Trailer wiring",
            "12N socket, relay pack and towing module.",
            listOf(
                "Terminate with a dedicated relay pack, not by scotchlocks into the lamp circuits.",
                "A CAN-bus vehicle needs a coded towing module or the trailer lamps will fault.",
                "Check the dedicated trailer earth to the chassis, not the lamp earth.",
            ),
        ),
    )

    val pinouts: List<Pinout> = listOf(
        Pinout(
            "obd2",
            "OBD-II (J1962) 16-pin",
            "Standard diagnostic socket, ISO 15765 / SAE J1962.",
            listOf(
                "1" to "Manufacturer specific",
                "2" to "Bus + (SAE J1850 PWM/VPW)",
                "3" to "Manufacturer specific",
                "4" to "Chassis ground",
                "5" to "Signal ground",
                "6" to "CAN High (ISO 15765-4)",
                "7" to "K-line (ISO 9141-2 / KWP2000)",
                "8" to "Manufacturer specific",
                "9" to "Manufacturer specific",
                "10" to "Bus - (SAE J1850 PWM/VPW)",
                "11" to "Manufacturer specific",
                "12" to "Manufacturer specific",
                "13" to "Manufacturer specific",
                "14" to "CAN Low (ISO 15765-4)",
                "15" to "L-line (ISO 9141-2)",
                "16" to "Battery positive (permanent 12 V)",
            ),
        ),
        Pinout(
            "iso-relay-5",
            "ISO mini relay (5-pin)",
            "Standard automotive changeover relay, ISO 7588.",
            listOf(
                "30" to "Common feed (permanent or switched 12 V)",
                "85" to "Coil negative (usually ECU-switched earth)",
                "86" to "Coil positive (12 V)",
                "87" to "Normally-open output (energised)",
                "87a" to "Normally-closed output (rest, 5-pin only)",
            ),
        ),
        Pinout(
            "iso-relay-4",
            "ISO mini relay (4-pin)",
            "Standard automotive make relay.",
            listOf(
                "30" to "Common feed",
                "85" to "Coil earth",
                "86" to "Coil 12 V",
                "87" to "Output when energised",
            ),
        ),
        Pinout(
            "trailer-12n",
            "Trailer socket 12N (7-pin)",
            "Standard towing lighting socket, ISO 1724.",
            listOf(
                "1" to "Left indicator (yellow)",
                "2" to "Rear fog (blue) / spare",
                "3" to "Earth (white)",
                "4" to "Right indicator (green)",
                "5" to "Right tail (brown)",
                "6" to "Brake lights (red)",
                "7" to "Left tail (black)",
            ),
        ),
        Pinout(
            "injector-2",
            "Fuel injector (2-pin)",
            "Saturated or peak-and-hold injector connector.",
            listOf(
                "1" to "12 V feed (via fuse/relay from main relay)",
                "2" to "ECU switched earth (pulsed)",
            ),
        ),
        Pinout(
            "cop-4",
            "Coil-on-plug (4-pin)",
            "Smart coil with integrated igniter.",
            listOf(
                "1" to "Ignition trigger from ECU",
                "2" to "Engine ground",
                "3" to "12 V feed from main relay",
                "4" to "Ignition feedback / diagnostic (if used)",
            ),
        ),
        Pinout(
            "maf-5",
            "Mass air flow sensor (5-pin)",
            "Hot-film MAF, typical analogue or digital output.",
            listOf(
                "1" to "Sensor ground",
                "2" to "12 V feed from main relay",
                "3" to "Signal output to ECU",
                "4" to "Intake air temperature signal (if combined)",
                "5" to "Reference / shield",
            ),
        ),
        Pinout(
            "o2-4",
            "Heated oxygen sensor (4-wire)",
            "Zirconia lambda sensor with heater.",
            listOf(
                "1" to "Heater +12 V",
                "2" to "Heater earth (ECU switched)",
                "3" to "Sensor signal to ECU",
                "4" to "Sensor signal ground",
            ),
        ),
    )

    /** DIN 72552 / German colour abbreviations used across European diagrams. */
    val colours: List<WireColour> = listOf(
        WireColour("ws", "White", "weiß"),
        WireColour("sw", "Black", "schwarz"),
        WireColour("rt", "Red", "rot"),
        WireColour("gn", "Green", "grün"),
        WireColour("bl", "Blue", "blau"),
        WireColour("gr", "Grey", "grau"),
        WireColour("ge", "Yellow", "gelb"),
        WireColour("br", "Brown", "braun"),
        WireColour("vi", "Violet", "violett"),
        WireColour("li", "Lilac", "lila"),
        WireColour("or", "Orange", "orange"),
        WireColour("rs", "Pink", "rosa"),
    )

    /** DIN 72552 terminal numbers a technician meets on every diagram. */
    val terminals: List<Pair<String, String>> = listOf(
        "15" to "Switched positive after ignition switch",
        "30" to "Permanent positive, direct from battery",
        "31" to "Earth / ground return to battery negative",
        "50" to "Starter control (cranking signal)",
        "53" to "Wiper motor, park position",
        "54" to "Brake light switch output",
        "56" to "Headlamp feed",
        "61" to "Charge warning lamp from alternator",
        "85" to "Relay coil negative",
        "86" to "Relay coil positive",
        "87" to "Relay output, normally open",
    )

    fun search(query: String): Pair<List<WiringSystem>, List<Pinout>> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return systems to pinouts
        val sys = systems.filter { system ->
            system.name.lowercase().contains(q) ||
                system.summary.lowercase().contains(q) ||
                system.checks.any { it.lowercase().contains(q) }
        }
        val pins = pinouts.filter { pinout ->
            pinout.name.lowercase().contains(q) ||
                pinout.description.lowercase().contains(q) ||
                pinout.pins.any { it.first.lowercase().contains(q) || it.second.lowercase().contains(q) }
        }
        return sys to pins
    }
}
