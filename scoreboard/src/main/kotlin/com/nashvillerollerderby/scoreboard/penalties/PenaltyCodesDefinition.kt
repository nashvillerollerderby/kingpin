package com.nashvillerollerderby.scoreboard.penalties

class PenaltyCodesDefinition {
    private var penalties: MutableList<PenaltyCode>? = null

    fun getPenalties(): List<PenaltyCode>? {
        return penalties
    }

    fun setPenalties(penalties: MutableList<PenaltyCode>?) {
        this.penalties = penalties
    }

    fun add(code: PenaltyCode) {
        penalties!!.add(code)
    }
}
