package com.nashvillerollerderby.scoreboard.utils

import java.util.regex.Pattern

object ClockConversion {
    @JvmStatic
    fun fromHumanReadable(v: String): Long? {
        val m = p.matcher(v)
        if (m.matches()) {
            val mr = m.toMatchResult()
            val min = mr.group(1).toLong()
            val sec = mr.group(2).toLong()
            var par: Long = 0
            if (mr.group(4) != null) {
                val parPad = mr.group(4) + "000"
                par = parPad.substring(0, 3).toLong()
            }

            return ((sec + (min * 60)) * 1000) + par
        }

        return try {
            v.toLong()
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun toHumanReadable(v: Long): String {
        val minutes = v / 1000 / 60
        val seconds = (v / 1000) % 60
        val partial = v % 1000

        return if (partial == 0L) {
            String.format("%d:%02d", minutes, seconds)
        } else {
            String.format("%d:%02d.%02d", minutes, seconds, partial)
        }
    }

    private val p: Pattern = Pattern.compile("(\\d+):(\\d+)(\\.(\\d+))?")
}
