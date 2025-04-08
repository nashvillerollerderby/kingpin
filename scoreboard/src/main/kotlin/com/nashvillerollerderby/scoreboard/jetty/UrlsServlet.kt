package com.nashvillerollerderby.scoreboard.jetty

import org.eclipse.jetty.server.NetworkConnector
import org.eclipse.jetty.server.Server
import java.io.IOException
import java.net.*
import java.util.*
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServlet

class UrlsServlet(protected var server: Server) : HttpServlet() {
    @get:Throws(MalformedURLException::class, SocketException::class)
    val urls: Set<String>
        get() {
            val urls: MutableSet<String> = TreeSet()
            for (c in server.connectors) {
                if (c is NetworkConnector) {
                    addURLs(urls, c.host, c.localPort)
                }
            }
            return urls
        }

    @Throws(MalformedURLException::class, SocketException::class)
    fun addURLs(urls: MutableSet<String>, host: String?, port: Int) {
        if (null == host) {
            for (iface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (addr in Collections.list(iface.inetAddresses)) {
                    if (!addr.isLoopbackAddress) {
                        urls.add(URL("http", addr.hostAddress, port, "/").toString())
                    }
                }
            }
        } else {
            urls.add(URL("http", host, port, "/").toString())
            try {
                // Get the IP address of the given host.
                urls.add(URL("http", InetAddress.getByName(host).hostAddress, port, "/").toString())
            } catch (uhE: UnknownHostException) {
            }
        }
    }

    @Throws(ServletException::class, IOException::class)
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        response.setHeader("Cache-Control", "no-cache")
        response.setHeader("Expires", "-1")
        response.characterEncoding = "UTF-8"

        try {
            response.contentType = "text/plain"
            for (u in urls) {
                response.writer.println(u)
            }
            response.status = HttpServletResponse.SC_OK
        } catch (muE: MalformedURLException) {
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Could not parse internal URL : " + muE.message
            )
        } catch (sE: SocketException) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket Exception : " + sE.message)
        }
    }
}
