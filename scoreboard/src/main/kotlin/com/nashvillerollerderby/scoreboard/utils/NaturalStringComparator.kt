package com.nashvillerollerderby.scoreboard.utils

class NaturalStringComparator : Comparator<String> {
    override fun compare(s1: String, s2: String): Int {
        // Skip all identical characters
        val len1 = s1.length
        val len2 = s2.length
        var c1: Char
        var c2: Char
        var i = 0
        c1 = 0.toChar()
        c2 = 0.toChar()
        while ((i < len1) && (i < len2) && (s1[i].also { c1 = it }) == (s2[i].also { c2 = it })) {
            i++
        }

        // Check end of string
        if (c1 == c2) return (len1 - len2)

        // Check digit in first string
        if (Character.isDigit(c1)) {
            // Check digit only in first string
            if (!Character.isDigit(c2)) return (1)

            // Scan all integer digits
            var x1 = i + 1
            while ((x1 < len1) && Character.isDigit(s1[x1])) {
                x1++
            }
            var x2 = i + 1
            while ((x2 < len2) && Character.isDigit(s2[x2])) {
                x2++
            }

            // Longer integer wins, first digit otherwise
            return (if (x2 == x1) c1.code - c2.code else x1 - x2)
        }

        // Check digit only in second string
        if (Character.isDigit(c2)) return (-1)

        // No digits
        return (c1.code - c2.code)
    }
}
