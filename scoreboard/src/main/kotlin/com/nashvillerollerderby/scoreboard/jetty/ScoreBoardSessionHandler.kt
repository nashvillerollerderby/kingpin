package com.nashvillerollerderby.scoreboard.jetty

import com.nashvillerollerderby.scoreboard.core.interfaces.Clients
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.session.SessionHandler
import jakarta.servlet.http.HttpServletRequest

class ScoreBoardSessionHandler(sb: ScoreBoard) : SessionHandler() {
    public override fun checkRequestedSessionId(baseRequest: Request, request: HttpServletRequest) {
        super.checkRequestedSessionId(baseRequest, request)
        baseRequest.sessionHandler = this
        baseRequest.getSession(true)
    }

    @Throws(Exception::class)
    override fun isIdInUse(id: String): Boolean {
        return super.isIdInUse(id) || clients.getDevice(id) != null
    }

    private val clients: Clients = sb.clients
}
