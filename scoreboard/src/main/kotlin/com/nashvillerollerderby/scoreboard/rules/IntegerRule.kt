package com.nashvillerollerderby.scoreboard.rules

class IntegerRule(fullname: String?, description: String?, defaultValue: Int) :
    RuleDefinition(Type.INTEGER, fullname, description, defaultValue) {
    override fun isValueValid(v: String): Boolean {
        try {
            v.toInt()
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
