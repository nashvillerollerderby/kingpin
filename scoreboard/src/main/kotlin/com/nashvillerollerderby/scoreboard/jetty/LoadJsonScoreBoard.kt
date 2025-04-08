package com.nashvillerollerderby.scoreboard.jetty

import com.fasterxml.jackson.jr.ob.JSON
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider
import com.nashvillerollerderby.scoreboard.json.ScoreBoardJSONSetter
import com.nashvillerollerderby.scoreboard.utils.StatsbookImporter
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.fileupload2.core.FileUploadException
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicInteger

class LoadJsonScoreBoard(protected val scoreBoard: ScoreBoard) : HttpServlet() {
    @Throws(ServletException::class, IOException::class)
    override fun doPost(request: HttpServletRequest, response: HttpServletResponse) {
        if (scoreBoard.clients.getDevice(request.session.id).mayWrite()) {
            scoreBoard.clients.getDevice(request.session.id).write()
            scoreBoard.runInBatch {
                scoreBoard.set(
                    ScoreBoard.IMPORTS_IN_PROGRESS,
                    1,
                    ScoreBoardEventProvider.Flag.CHANGE
                )
            }
            try {
                if (!JakartaServletFileUpload.isMultipartContent(request)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST)
                    return
                }

                val sfU = JakartaServletFileUpload()
                val items = sfU.getItemIterator(request)
                while (items.hasNext()) {
                    val item = items.next()
                    if (!item.isFormField) {
                        if (request.pathInfo.equals("/JSON", ignoreCase = true)) {
                            runningImports.incrementAndGet()
                            val stream = item.inputStream
                            val map = JSON.std.mapFrom(stream)
                            stream.close()
                            val state = map["state"] as MutableMap<String, Any>?
                            ScoreBoardJSONSetter.updateToCurrentVersion(state)
                            scoreBoard.runInBatch {
                                ScoreBoardJSONSetter.set(
                                    scoreBoard,
                                    state,
                                    ScoreBoardEventProvider.Source.JSON
                                )
                            }
                            runningImports.decrementAndGet()
                            response.contentType = "text/plain"
                            response.status = HttpServletResponse.SC_OK

                            synchronized(runningImports) {
                                if (runningImports.get() == 0) {
                                    scoreBoard.cleanupAliases()
                                }
                            }
                        } else if (request.pathInfo.equals("/xlsx", ignoreCase = true)) {
                            sbImporter.read(item.inputStream)
                        } else if (request.pathInfo.equals("/blank_xlsx", ignoreCase = true)) {
                            val outputPath = Paths.get("blank_statsbook.xlsx")
                            Files.copy(item.inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING)
                            scoreBoard.runInBatch {
                                scoreBoard.settings[ScoreBoard.SETTING_STATSBOOK_INPUT] = outputPath.toString()
                            }
                        }
                        return
                    }
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No File uploaded")
            } catch (fuE: FileUploadException) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, fuE.message)
            } catch (iE: IOException) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error Reading File: " + iE.message)
            } finally {
                scoreBoard.runInBatch {
                    scoreBoard.set(
                        ScoreBoard.IMPORTS_IN_PROGRESS,
                        -1,
                        ScoreBoardEventProvider.Flag.CHANGE
                    )
                }
            }
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No write access")
        }
    }

    protected val sbImporter: StatsbookImporter = StatsbookImporter(scoreBoard)

    companion object {
        protected var runningImports: AtomicInteger = AtomicInteger(0)
    }
}
