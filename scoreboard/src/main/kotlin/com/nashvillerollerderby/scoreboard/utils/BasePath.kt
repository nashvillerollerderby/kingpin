package com.nashvillerollerderby.scoreboard.utils

import java.io.File

object BasePath {
    @JvmStatic
    fun get(): File {
        return basePath
    }

    fun set(f: File) {
        basePath = f
    } // for unit tests

    private var basePath = File(".")
}
