package com.workshop.manualorganiser.util

import com.workshop.manualorganiser.data.Manual

/**
 * Rule-based classifier and structure engine behind "AI Sort & Structure".
 *
 * It is deliberately transparent: every suggestion carries the keyword evidence
 * that produced it, so the result can be checked rather than trusted. When an AI
 * endpoint is configured, [AiClient] can enrich the text answer, but the taxonomy
 * decision itself stays local and deterministic.
 */
object Taxonomy {

    data class Category(
        val name: String,
        val keywords: List<String>,
        val sectionOrder: List<String>,
    )

    val categories: List<Category> = listOf(
        Category(
            "Engine",
            listOf(
                "engine", "2jz", "1jz", "ls1", "ls3", "s54", "n54", "ej20", "ej25",
                "cylinder", "piston", "crank", "camshaft", "timing", "head gasket",
                "turbo", "supercharger", "injector", "spark plug", "valve", "oil pump",
                "misfire", "compression", "torque spec",
            ),
            listOf("Specifications", "Torque settings", "Removal", "Strip-down", "Reassembly", "Diagnostics"),
        ),
        Category(
            "Electrical",
            listOf(
                "wiring", "electrical", "circuit", "fuse", "relay", "ground", "earth",
                "battery", "alternator", "starter", "charging", "sensor", "ecu", "ecm",
                "can bus", "obd", "pinout", "connector", "loom", "harness", "voltage",
            ),
            listOf("Wiring overview", "Connector pinouts", "Fuse & relay layout", "Diagnostics", "Component tests"),
        ),
        Category(
            "Transmission",
            listOf(
                "transmission", "gearbox", "clutch", "manual gearbox", "auto", "atf",
                "torque converter", "differential", "driveshaft", "gearbox oil",
                "synchro", "selector",
            ),
            listOf("Specifications", "Fluid & capacity", "Removal", "Reassembly", "Diagnostics"),
        ),
        Category(
            "Brakes",
            listOf(
                "brake", "abs", "caliper", "disc", "rotor", "pad", "master cylinder",
                "servo", "bleeding", "brake fluid", "handbrake", "parking brake",
            ),
            listOf("Specifications", "Inspection limits", "Replacement", "Bleeding procedure", "ABS diagnostics"),
        ),
        Category(
            "Suspension",
            listOf(
                "suspension", "shock", "strut", "spring", "bush", "bushing", "ball joint",
                "wishbone", "control arm", "alignment", "caster", "camber", "toe",
                "steering", "power steering", "rack",
            ),
            listOf("Specifications", "Torque settings", "Removal", "Reassembly", "Alignment data"),
        ),
        Category(
            "Body & Interior",
            listOf(
                "body", "panel", "bumper", "door", "trim", "interior", "seat", "glass",
                "windscreen", "windshield", "paint", "rust", "welding", "sunroof",
            ),
            listOf("Panel gaps", "Removal", "Repair", "Refitting", "Torque settings"),
        ),
        Category(
            "Safety",
            listOf(
                "safety", "airbag", "srs", "seatbelt", "pretensioner", "impact",
                "shop rules", "ppe", "hazard", "lift", "jack",
            ),
            listOf("Warnings", "Isolation procedure", "Inspection", "Torque settings"),
        ),
        Category(
            "Diagnostics",
            listOf(
                "diagnostic", "fault code", "dtc", "troubleshoot", "scan tool",
                "live data", "freeze frame", "p0", "p1", "b1", "u0",
            ),
            listOf("Fault codes", "Live data", "Guided tests", "Repair verification"),
        ),
        Category(
            "General",
            listOf("service", "maintenance", "schedule", "general", "overview", "introduction"),
            listOf("Overview", "Service schedule", "Specifications", "Notes"),
        ),
    )

    data class Suggestion(
        val category: String,
        val confidence: Float,
        val matched: List<String>,
        val sections: List<String>,
    )

    /** Classifies a manual and proposes a section structure. */
    fun suggest(manual: Manual): Suggestion {
        val haystack = manual.classificationText().lowercase()
        val scores = categories.associate { category ->
            category.name to category.keywords.filter { haystack.contains(it) }
        }
        val generalSections = categories.last().sectionOrder
        val best = scores.maxByOrNull { it.value.size }
        if (best == null || best.value.isEmpty()) {
            // Nothing distinctive was found, so stay in General rather than
            // guessing the first category in the list.
            return Suggestion(
                category = "General",
                confidence = 0f,
                matched = emptyList(),
                sections = generalSections,
            )
        }

        // Confidence blends the number of hits with how distinctive they are.
        val hits = best.value.size
        val confidence = (hits / (hits + 3f)).coerceIn(0f, 0.95f)

        val category = categories.first { it.name == best.key }

        // Detect a diagnostics sub-theme so a diagnostic manual gets that extra section.
        val sectionOrder = category.sectionOrder.toMutableList()
        if (scores["Diagnostics"]?.isNotEmpty() == true && !sectionOrder.contains("Diagnostics")) {
            sectionOrder.add("Diagnostics")
        }

        return Suggestion(
            category = category.name,
            confidence = confidence,
            matched = best.value.distinct().take(8),
            sections = sectionOrder,
        )
    }

    /**
     * Reorders pages so that pages whose labels mention a section come first,
     * grouped by the order proposed by [suggest]. Unlabelled pages keep their
     * relative order at the end.
     */
    fun orderPages(manual: Manual, sections: List<String>): List<com.workshop.manualorganiser.data.ManualPage> {
        val buckets = sections.associateWith { mutableListOf<com.workshop.manualorganiser.data.ManualPage>() }
        val unassigned = mutableListOf<com.workshop.manualorganiser.data.ManualPage>()

        manual.pages.forEach { page ->
            val label = page.label.lowercase()
            val bucket = if (label.isBlank()) {
                null
            } else {
                sections.firstOrNull { section ->
                    val token = section.lowercase().substringBefore(" &")
                    label.contains(token)
                }
            }
            if (bucket != null) buckets.getValue(bucket).add(page) else unassigned.add(page)
        }

        return sections.flatMap { buckets.getValue(it) } + unassigned
    }
}
