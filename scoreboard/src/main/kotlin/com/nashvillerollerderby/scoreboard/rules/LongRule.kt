package com.nashvillerollerderby.scoreboard.rules

class LongRule(fullname: String?, description: String?, defaultValue: Int) :
    RuleDefinition(Type.LONG, fullname, description, defaultValue) {
    override fun isValueValid(v: String): Boolean {
        try {
            v.toLong()
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
