package com.nashvillerollerderby.scoreboard.core.interfaces

enum class FloorPosition(val role: Role, private val string: String) {
    JAMMER(Role.JAMMER, "Jammer"),
    PIVOT(Role.PIVOT, "Pivot"),  // used as Blocker4, if no Pivot fielded
    BLOCKER1(Role.BLOCKER, "Blocker1"),
    BLOCKER2(Role.BLOCKER, "Blocker2"),
    BLOCKER3(Role.BLOCKER, "Blocker3");

    fun getRole(teamJam: TeamJam?): Role {
        if (teamJam == null) {
            return role
        }
        return if (role == Role.PIVOT && teamJam.isStarPass) {
            Role.JAMMER
        } else if (role == Role.JAMMER && teamJam.isStarPass || role == Role.PIVOT && teamJam.hasNoPivot()) {
            Role.BLOCKER
        } else {
            role
        }
    }

    override fun toString(): String {
        return string
    }

    companion object {
        @JvmStatic
        fun fromString(s: String): FloorPosition? {
            for (fp in entries) {
                if (fp.toString() == s) {
                    return fp
                }
            }
            return null
        }
    }
}
