package com.nashvillerollerderby.scoreboard.rules

class StringRule(fullname: String?, description: String?, defaultValue: String) :
    RuleDefinition(Type.STRING, fullname, description, defaultValue) {
    override fun isValueValid(v: String?): Boolean {
        return v != null
    }
}
