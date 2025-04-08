package com.nashvillerollerderby.scoreboard.jetty

import com.fasterxml.jackson.jr.ob.JSON
import com.nashvillerollerderby.scoreboard.core.game.GameImpl
import com.nashvillerollerderby.scoreboard.core.interfaces.*
import com.nashvillerollerderby.scoreboard.core.interfaces.Clients.Device
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider
import com.nashvillerollerderby.scoreboard.json.JSONStateListener
import com.nashvillerollerderby.scoreboard.json.JSONStateListener.PathTrie
import com.nashvillerollerderby.scoreboard.json.JSONStateListener.StateTrie
import com.nashvillerollerderby.scoreboard.json.JSONStateManager
import com.nashvillerollerderby.scoreboard.json.ScoreBoardJSONSetter
import com.nashvillerollerderby.scoreboard.json.ScoreBoardJSONSetter.JSONSet
import com.nashvillerollerderby.scoreboard.utils.Log4j2Logging
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory

class WS(private val sb: ScoreBoard, private val jsm: JSONStateManager, private val useMetrics: Boolean) :
    JettyWebSocketServlet() {

    private val logger = Log4j2Logging.getLogger(this)

    override fun configure(factory: JettyWebSocketServletFactory) {
        factory.setCreator(ScoreBoardWebSocketCreator())
    }

    private fun hasPermission(device: Device, action: String): Boolean {
        return when (action) {
            "Register", "Ping" -> true
            "Set", "StartNewGame" -> device.mayWrite()
            else -> device.mayWrite()
        }
    }

    private var connectionsActive: Gauge? = null
    private var messagesReceived: Counter? = null
    private var messagesSentDuration: Histogram? = null
    private var messagesSentFailures: Counter? = null

    init {
        if (useMetrics) {
            connectionsActive =
                Gauge.build().name("crg_websocket_active_connections").help("Current WebSocket connections").register()
            messagesReceived = Counter.build()
                .name("crg_websocket_messages_received")
                .help("Number of WebSocket messages received")
                .register()
            messagesSentDuration = Histogram.build()
                .name("crg_websocket_messages_sent_duration_seconds")
                .help("Time spent sending WebSocket messages")
                .register()
            messagesSentFailures = Counter.build()
                .name("crg_websocket_messages_sent_failed")
                .help("Number of WebSocket messages we failed to send")
                .register()
        } else {
            connectionsActive = null
            messagesReceived = null
            messagesSentDuration = null
            messagesSentFailures = null
        }
    }

    inner class ScoreBoardWebSocketCreator : JettyWebSocketCreator {
        override fun createWebSocket(request: JettyServerUpgradeRequest, response: JettyServerUpgradeResponse): Any {
            val baseRequest = request.httpServletRequest
            val httpSessionId = baseRequest.session.id
            val remoteAddress = baseRequest.remoteAddr
            var source = baseRequest.getParameter("source")
            if (source == null) {
                source = "CUSTOM CLIENT"
            }
            var platform = baseRequest.getParameter("platform")
            if (platform == null) {
                platform = baseRequest.getHeader("User-Agent")
            }
            return ScoreBoardWebSocket(httpSessionId, remoteAddress, source, platform)
        }
    }

    @WebSocket(maxTextMessageSize = 1024 * 1024)
    inner class ScoreBoardWebSocket(
        httpSessionId: String?,
        remoteAddress: String?,
        source: String?,
        platform: String?
    ) :
        JSONStateListener {
        @OnWebSocketMessage
        @Synchronized
        fun onMessage(session: Session?, messageData: String?) {
            if (useMetrics) {
                messagesReceived!!.inc()
            }
            try {
                var json = JSON.std.mapFrom(messageData)
                val action = json["action"] as String?
                if (!hasPermission(device, action!!)) {
                    json = HashMap()
                    json["authorization"] = "Not authorized for $action"
                    send(json)
                    return
                }
                when (action) {
                    "Register" -> {
                        val jsonPaths = json["paths"] as List<*>?
                        if (jsonPaths != null) {
                            val newPaths = PathTrie()
                            for (p in jsonPaths) {
                                newPaths.add(p as String?)
                            }
                            sendWSUpdates(newPaths, state)
                            paths.merge(newPaths)
                        }
                    }

                    "Set" -> {
                        sbClient.write()
                        val key = json["key"] as String?
                        val value = json["value"]
                        val v = value?.toString()
                        var flag: ScoreBoardEventProvider.Flag? = null
                        val f = json["flag"] as String?
                        if ("reset" == f) {
                            flag = ScoreBoardEventProvider.Flag.RESET
                        }
                        if ("change" == f) {
                            flag = ScoreBoardEventProvider.Flag.CHANGE
                        }
                        val js = JSONSet(key, v, flag)
                        sb.runInBatch {
                            ScoreBoardJSONSetter.set(
                                sb,
                                listOf(js),
                                ScoreBoardEventProvider.Source.WS
                            )
                        }
                    }

                    "StartNewGame" -> {
                        sbClient.write()
                        val data = json["data"] as Map<String, Any>?
                        sb.runInBatch(object : Runnable {
                            override fun run() {
                                val t1 = sb.getPreparedTeam(data!!["Team1"] as String?)
                                val t2 = sb.getPreparedTeam(data["Team2"] as String?)
                                val rs = sb.rulesets.getRuleset(data["Ruleset"] as String?)
                                val g = GameImpl(sb, t1, t2, rs)
                                sb.add(ScoreBoard.GAME, g)
                                sb.currentGame.load(g)

                                if ((data["Advance"] as Boolean?)!!) {
                                    g.allowQuickClockControls(true)
                                    g.startJam()
                                    g.timeout()
                                    run {
                                        var i = 0
                                        while (i < (data["TO1"] as Int?)!!) {
                                            g.setTimeoutType(g.getTeam(Team.ID_1), false)
                                            g.getClock(Clock.ID_TIMEOUT)
                                                .elapseTime(1000) // avoid double click detection
                                            g.timeout()
                                            i++
                                        }
                                    }
                                    run {
                                        var i = 0
                                        while (i < (data["TO2"] as Int?)!!) {
                                            g.setTimeoutType(g.getTeam(Team.ID_2), false)
                                            g.getClock(Clock.ID_TIMEOUT)
                                                .elapseTime(1000) // avoid double click detection
                                            g.timeout()
                                            i++
                                        }
                                    }
                                    run {
                                        var i = 0
                                        while (i < (data["OR1"] as Int?)!!) {
                                            g.setTimeoutType(g.getTeam(Team.ID_1), true)
                                            g.getClock(Clock.ID_TIMEOUT)
                                                .elapseTime(1000) // avoid double click detection
                                            g.timeout()
                                            i++
                                        }
                                    }
                                    var i = 0
                                    while (i < (data["OR2"] as Int?)!!) {
                                        g.setTimeoutType(g.getTeam(Team.ID_2), true)
                                        g.getClock(Clock.ID_TIMEOUT).elapseTime(1000) // avoid double click detection
                                        g.timeout()
                                        i++
                                    }
                                    g.setTimeoutType(Timeout.Owners.OTO, false)
                                    g.getTeam(Team.ID_1).set(Team.TRIP_SCORE, data["Points1"] as Int?)
                                    g.getTeam(Team.ID_2).set(Team.TRIP_SCORE, data["Points2"] as Int?)
                                    val period = data["Period"] as Int
                                    val jam = data["Jam"] as Int
                                    if (jam == 0 && period > 1) {
                                        g.getClock(Clock.ID_PERIOD)
                                            .elapseTime(g.getClock(Clock.ID_PERIOD).maximumTime + 1000)
                                        g.stopJamTO()
                                        g.getClock(Clock.ID_INTERMISSION)
                                            .elapseTime(g.getClock(Clock.ID_INTERMISSION).maximumTime + 1000)
                                        var j = 2
                                        while (j < period) {
                                            g.currentPeriod.execute(Period.INSERT_BEFORE)
                                            j++
                                        }
                                    } else {
                                        run {
                                            var j = 1
                                            while (j < period) {
                                                g.currentPeriod.execute(Period.INSERT_BEFORE)
                                                j++
                                            }
                                        }
                                        var j = 1
                                        while (j < jam) {
                                            g.currentPeriod.currentJam.execute(Jam.INSERT_BEFORE)
                                            j++
                                        }
                                    }
                                    val periodClock = (data["PeriodClock"] as String?)!!.toLong()
                                    if (periodClock > 0) {
                                        g.getClock(Clock.ID_PERIOD).time =
                                            periodClock
                                    }
                                    g.allowQuickClockControls(false)
                                } else {
                                    val intermissionClock = data["IntermissionClock"] as String?
                                    if (intermissionClock != null) {
                                        var ic_time = intermissionClock.toLong()
                                        ic_time = ic_time - (ic_time % 1000)
                                        val c = g.getClock(Clock.ID_INTERMISSION)
                                        c.maximumTime = ic_time
                                        c.restart()
                                    }
                                }
                            }
                        })
                    }

                    "Ping" -> {
                        json = HashMap()
                        json["Pong"] = ""
                        send(json)

                        // This is usually only every 30s, so often enough
                        // to cover us if our process is terminated uncleanly
                        // without having to build something just for it
                        // or risking an update loop.
                        device.access()
                    }

                    else -> sendError("Unknown Action '$action'")
                }
            } catch (je: Exception) {
                logger.error("Error handling JSON message:", je)
            }
        }

        fun send(json: Map<String?, Any?>?) {
            val timer = if (useMetrics) messagesSentDuration!!.startTimer() else null
            try {
                wsSession!!.remote.sendString(
                    JSON.std.with(JSON.Feature.WRITE_NULL_PROPERTIES).composeString().addObject(json).finish()
                )
            } catch (e: Exception) {
                logger.error("Error sending JSON update:", e)
                if (useMetrics) {
                    messagesSentFailures!!.inc()
                }
            } finally {
                if (useMetrics) {
                    timer!!.observeDuration()
                }
            }
        }

        @OnWebSocketConnect
        fun onOpen(session: Session) {
            wsSession = session
            if (useMetrics) {
                connectionsActive!!.inc()
            }
            jsm.register(this)
            device.access()

            val json: MutableMap<String?, Any?> = HashMap()
            val initialState: MutableMap<String, Any> = HashMap()
            // Inject some of our own WS-specific information.
            // Session id is not included, as that's the secret cookie which
            // is meant to be httpOnly.
            initialState["WS.Device.Id"] = device.id
            initialState["WS.Device.Name"] = device.name
            initialState["WS.Client.Id"] = sbClient.id
            initialState["WS.Client.RemoteAddress"] = session.remoteAddress.toString()
            json["state"] = initialState
            send(json)
        }

        @OnWebSocketClose
        fun onClose(closeCode: Int, message: String?) {
            if (useMetrics) {
                connectionsActive!!.dec()
            }
            jsm.unregister(this)
            sb.clients.removeClient(sbClient)

            device.access()
        }

        fun sendError(message: String?) {
            val json: MutableMap<String?, Any?> = HashMap()
            json["error"] = message
            send(json)
        }

        // State changes from JSONStateManager.
        @Synchronized
        override fun sendUpdates(fullState: StateTrie, changedState: StateTrie) {
            this.state = fullState
            sendWSUpdates(paths, changedState)
        }

        @Synchronized
        private fun sendWSUpdates(registered: PathTrie, updated: StateTrie) {
            val updates = registered.intersect(updated, true)
            if (updates.isEmpty()) {
                return
            }
            val json: MutableMap<String?, Any?> = HashMap()
            json["state"] = updates
            send(json)
        }

        private var sbClient: Clients.Client
        private var device: Device = sb.clients.getOrAddDevice(httpSessionId)
        private var paths: PathTrie = PathTrie()
        private var state = StateTrie()
        private var wsSession: Session? = null

        init {
            sbClient = sb.clients.addClient(device.id, remoteAddress, source, platform)
        }
    }
}
