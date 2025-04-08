package com.nashvillerollerderby.scoreboard.penalties

import com.nashvillerollerderby.scoreboard.event.ValueWithId

class PenaltyCode : ValueWithId {
    var code: String? = null

    @JvmField
    var verbalCues: List<String>? = null

    constructor()

    constructor(code: String?, verbalCues: List<String>?) {
        this.code = code
        this.verbalCues = verbalCues
    }

    constructor(code: String?, vararg verbalCues: String) {
        this.code = code
        this.verbalCues = mutableListOf(*verbalCues)
    }

    override fun getId(): String {
        return code!!
    }

    override fun getValue(): String {
        return java.lang.String.join(",", verbalCues)
    }
}
