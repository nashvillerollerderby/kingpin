package com.nashvillerollerderby.scoreboard.jetty

import com.fasterxml.jackson.jr.ob.JSON
import com.nashvillerollerderby.scoreboard.json.JSONStateManager
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.util.*

class SaveJsonScoreBoard(private val jsm: JSONStateManager) : HttpServlet() {
    @Throws(ServletException::class, IOException::class)
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        // Use a TreeMap to keep output sorted.
        val state: MutableMap<String, Any> = TreeMap(jsm.state)

        val path = request.getParameter("path")
        if (path != null) {
            val prefixes = listOf(*path.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            val it: MutableIterator<String> = state.keys.iterator()
            while (it.hasNext()) {
                val key = it.next()
                var keep = key.startsWith("ScoreBoard.Version")
                for (prefix in prefixes) {
                    if (key.startsWith(prefix)) {
                        keep = true
                    }
                }
                if (!keep) {
                    it.remove()
                }
            }
        }
        // Users may use saves to share with the world, so remove secrets.
        val it: MutableIterator<String> = state.keys.iterator()
        while (it.hasNext()) {
            if (it.next().endsWith("Secret")) {
                it.remove()
            }
        }

        if (state.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No objects found.")
        } else {
            response.contentType = "application/json"
            val json = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                .composeString()
                .startObject()
                .putObject("state", state)
                .end()
                .finish()

            response.setHeader("Cache-Control", "no-cache")
            response.setHeader("Expires", "-1")
            response.characterEncoding = "utf-8"
            response.outputStream.print(json)
            response.outputStream.flush()
            response.status = HttpServletResponse.SC_OK
        }
    }
}
