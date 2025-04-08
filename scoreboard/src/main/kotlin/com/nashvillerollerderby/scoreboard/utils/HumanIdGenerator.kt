package com.nashvillerollerderby.scoreboard.utils

import java.util.*

object HumanIdGenerator {
    @JvmStatic
    fun generate(): String {
        // This intended to create a unique ID across ~100 active devices,
        // which is about the most a good WiFi AP can handle.
        // Given the birthday paradox, ~10k unique values are required.
        // This is intended to be more approachable than a UUID and a
        // bit of fun, not cryptographically secure.
        val i1 = rand.nextInt(terms.size)
        val i2 = rand.nextInt(terms.size)
        return if (i1 != i2) {
            terms[i1] + "-" + terms[i2]
        } else {
            terms[i1] + "-" + overflow[rand.nextInt(
                overflow.size
            )]
        }
    }

    // This avoids terms like "bench", "box", or "GTO", as that could be confused with a
    // tablet/laptop in that location or for that person.
    private val terms = arrayOf(
        "skater", "jammer", "pivot", "blocker", "alternate", "captain", "jam",
        "period", "timeout", "lineup", "team", "review", "start", "stop",
        "seconds", "whistle", "rolling", "stoppage", "clock", "tweet",

        "illegal", "violation", "target", "blocking", "zone", "position", "multiplayer",
        "pass", "penalty", "score", "trip", "point", "initial", "interference",
        "delay", "procedure", "expulsion", "gross", "foulout", "warning", "block",
        "gaining", "report", "return", "impact", "high", "low", "contact",
        "direction", "clockwise", "impenetrable", "pack", "split", "play", "out",
        "in", "skating", "destruction", "bounds", "failure", "yield", "miscounduct",
        "false", "line", "stay", "lead", "lost", "call", "engagement",
        "complete", "incomplete", "stand", "done", "overtime", "reentry", "insubordination",
        "unsporting", "cut", "swap", "spectrum",

        "head", "back", "shoulder", "knee", "toe", "torso", "finger",
        "leg", "chin", "thigh", "pads", "mouth", "guard", "wrist",
        "elbow", "forearm", "hand", "shin", "wheel", "truck", "star",
        "stripe", "helmet", "cover", "toestop", "face", "nose", "uniform",
        "number",

        "bridge", "goat", "wall", "tripod", "recycle", "runback", "lane",
        "power",

        "short", "flat", "banked", "minor", "major",
    )

    // If there's a duplicate, we take from this list.
    private val overflow = arrayOf("ball", "offside", "touchdown", "goalie", "racket", "grass")

    private val rand = Random()
}
