package com.nashvillerollerderby.scoreboard.rules

import com.nashvillerollerderby.scoreboard.utils.ClockConversion

class TimeRule(fullname: String?, description: String?, defaultValue: String) :
    RuleDefinition(Type.TIME, fullname, description, defaultValue) {
    override fun isValueValid(v: String): Boolean {
        return ClockConversion.fromHumanReadable(v) != null
    }
}
