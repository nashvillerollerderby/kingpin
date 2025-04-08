package com.nashvillerollerderby.scoreboard.utils

import com.nashvillerollerderby.scoreboard.event.ValueWithId

class ValWithId(private val id: String, private val value: String) : ValueWithId {
    override fun getId(): String {
        return id
    }

    override fun getValue(): String {
        return value
    }
}
