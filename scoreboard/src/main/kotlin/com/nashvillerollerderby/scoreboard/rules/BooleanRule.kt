package com.nashvillerollerderby.scoreboard.rules

class BooleanRule(
    fullname: String?,
    description: String?,
    defaultValue: Boolean,
    trueValue: String?,
    falseValue: String?
) :
    RuleDefinition(Type.BOOLEAN, fullname, description, defaultValue) {
    init {
        addProperties(boolProps)
        set(TRUE_VALUE, trueValue)
        set(FALSE_VALUE, falseValue)
        addWriteProtection(TRUE_VALUE)
        addWriteProtection(FALSE_VALUE)
    }

    override fun isValueValid(v: String): Boolean {
        try {
            v.toBoolean()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun toHumanReadable(v: Any?): String {
        if (v == null) {
            return ""
        }

        return if (v.toString().toBoolean()) trueValue else falseValue
    }

    val trueValue: String
        get() = get(TRUE_VALUE)
    val falseValue: String
        get() = get(FALSE_VALUE)
}
