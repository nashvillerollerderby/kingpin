package com.nashvillerollerderby.scoreboard.jetty

import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import com.nashvillerollerderby.scoreboard.json.JSONStateManager
import com.nashvillerollerderby.scoreboard.utils.BasePath
import com.nashvillerollerderby.scoreboard.utils.Log4j2Logging
import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.servlet.jakarta.exporter.MetricsServlet
import io.prometheus.client.servlet.jakarta.filter.MetricsFilter
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServlet
import org.eclipse.jetty.http.HttpCookie.SameSite
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import java.io.File
import java.net.MalformedURLException
import java.net.SocketException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JettyServletScoreBoardController(
    var scoreBoard: ScoreBoard,
    private var jsm: JSONStateManager,
    var host: String?,
    private var port: Int,
    useMetrics: Boolean
) {

    private val logger = Log4j2Logging.getLogger(this)

    protected fun init(useMetrics: Boolean) {
        server = Server()

        val sC = ServerConnector(server)
        sC.host = host
        sC.port = port
        server!!.addConnector(sC)

        val contexts = ContextHandlerCollection()
        server!!.handler = contexts

        val sch = ServletContextHandler(contexts, "/", ServletContextHandler.SESSIONS)

        val sessions: SessionHandler = ScoreBoardSessionHandler(scoreBoard)
        sessions.httpOnly = true
        sessions.sameSite = SameSite.LAX
        sessions.sessionCookie = "CRG_SCOREBOARD"
        sessions.sessionCookieConfig.maxAge = COOKIE_DURATION_SECONDS
        sessions.maxInactiveInterval = COOKIE_DURATION_SECONDS
        // Sessions are created per request, so they're actually refreshed on each
        // request which is harmless.
        sessions.refreshCookieAge = 1
        sch.sessionHandler = sessions
        // Only keep the first two path components.
        val mf = FilterHolder(
            MetricsFilter("jetty_http_request_latency_seconds", "Jetty HTTP request latency", 2, DoubleArray(1), false)
        )
        sch.addFilter(mf, "/*", EnumSet.of(DispatcherType.REQUEST))

        sch.resourceBase = (File(BasePath.get(), "html")).path
        val sh = ServletHolder(DefaultServlet())
        sh.setInitParameter("cacheControl", "no-cache")
        sh.setInitParameter("etags", "true")
        sch.addServlet(sh, "/*")

        urlsServlet = UrlsServlet(server!!)
        sch.addServlet(ServletHolder(urlsServlet), "/urls/*")

        ws = WS(scoreBoard, jsm, useMetrics)
        JettyWebSocketServletContainerInitializer.configure(sch, null)
        sch.addServlet(ServletHolder(ws), "/WS/*")

        if (useMetrics) {
            DefaultExports.initialize()
            metricsServlet = MetricsServlet()
            sch.addServlet(ServletHolder(metricsServlet), "/metrics")
        }

        val sjs: HttpServlet = SaveJsonScoreBoard(jsm)
        sch.addServlet(ServletHolder(sjs), "/SaveJSON/*")

        val ljs: HttpServlet = LoadJsonScoreBoard(scoreBoard)
        sch.addServlet(ServletHolder(ljs), "/Load/*")

        val ms: HttpServlet = MediaServlet(scoreBoard, File(BasePath.get(), "html").path)
        sch.addServlet(ServletHolder(ms), "/Media/*")
    }

    fun start() {
        try {
            server!!.start()
        } catch (e: Exception) {
            throw RuntimeException("Could not start server : $e")
        }

        logger.info("Open a web browser (either Google Chrome or Mozilla Firefox recommended) to:")
        logger.info("	http://" + (if (host != null) host else "localhost") + ":" + port)
        try {
            val urls = urlsServlet!!.urls.iterator()
            if (urls.hasNext()) {
                logger.info("or try one of these URLs:")
            }
            while (urls.hasNext()) {
                logger.info("	${urls.next()}")
            }
        } catch (muE: MalformedURLException) {
            logger.error("Internal error: malformed URL from Server Connector: ", muE)
        } catch (sE: SocketException) {
            logger.error("Internal error: socket exception from Server Connector: ", sE)
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            val removed =
                scoreBoard.clients.gcOldDevices(System.currentTimeMillis() - COOKIE_DURATION_SECONDS * 1000)
            if (removed > 0) {
                logger.debug("Garbage collected $removed old device(s).")
            }
        }, 0, 3600, TimeUnit.SECONDS)
    }

    private var server: Server? = null
    private var urlsServlet: UrlsServlet? = null
    var ws: WS? = null
    private var metricsServlet: MetricsServlet? = null

    init {
        init(useMetrics)
    }

    companion object {
        // No tournament lasts more than a week, so this allows plenty of time for
        // a device to be setup in advance and then only used as a backup on the
        // last day of the tournament. WFTDA/MRDA/JRDA allow 2 weeks to submit
        // stats after a sanctioned game, so this is also sufficient time to keep
        // things around in case it happens to help with forensics if something odd
        // is found while preparing the statsbook.
        const val COOKIE_DURATION_SECONDS: Int = 86400 * 15
    }
}
