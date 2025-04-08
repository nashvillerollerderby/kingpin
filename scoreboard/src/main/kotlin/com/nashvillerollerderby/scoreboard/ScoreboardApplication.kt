package com.nashvillerollerderby.scoreboard

import com.nashvillerollerderby.scoreboard.core.ScoreBoardImpl
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import com.nashvillerollerderby.scoreboard.jetty.JettyServletScoreBoardController
import com.nashvillerollerderby.scoreboard.json.AutoSaveJSONState
import com.nashvillerollerderby.scoreboard.json.ScoreBoardJSONListener
import com.nashvillerollerderby.scoreboard.utils.BasePath
import com.nashvillerollerderby.scoreboard.utils.Log4j2Logging
import com.nashvillerollerderby.scoreboard.utils.Version
import com.nashvillerollerderby.scoreboard.viewer.ScoreBoardMetricsCollector
import io.prometheus.client.Collector
import java.awt.Font
import java.awt.Toolkit
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.util.stream.Collectors
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import kotlin.system.exitProcess

class ScoreboardApplication(argv: Array<String>) {

    val logger = Log4j2Logging.getLogger(this)

    fun start() {
        try {
            if (!Version.load()) {
                stop(null)
            }
        } catch (e: IOException) {
            stop(e)
        }

        scoreBoard = ScoreBoardImpl(useMetrics)

        // JSON updates.
        val jsm = scoreBoard?.jsm
        ScoreBoardJSONListener(scoreBoard, jsm)

        // Controllers.
        val jetty =
            JettyServletScoreBoardController(scoreBoard!!, jsm!!, ApplicationConfig.config.host, ApplicationConfig.config.port, useMetrics)

        // Viewers.
        if (useMetrics) {
            ScoreBoardMetricsCollector(scoreBoard!!).register<Collector>()
        }

        val autoSaveDir = File(BasePath.get(), "config/autosave")
        scoreBoard?.runInBatch({
            if (!AutoSaveJSONState.loadAutoSave(scoreBoard, autoSaveDir)) {
                logger.info("No autosave to load from, using builtin defaults only")
            }
            scoreBoard?.postAutosaveUpdate()
        })

        // Only start auto-saves once everything is loaded in.
        val autosaver = AutoSaveJSONState(jsm, autoSaveDir, useMetrics)
        try {
            jetty.start()
        } catch (e: Throwable) {
            logger.error("Could not start server")
            stop(e)
        }

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                // Save any changes since last regular autosave before we shutdown.
                autosaver.run()
                logger.warn("Stopping application")
            }
        })
    }

    private fun stop(ex: Throwable?) {
        if (ex != null) {
            logger.error(ex)
        }
        logger.error("${LocalDateTime.now()} : Fatal error.   Exiting in 15 seconds.")
        try {
            Thread.sleep(15000)
        } catch (e: Exception) { /* Probably Ctrl-C or similar, ignore. */
        }
        exitProcess(1)
    }

    private fun parseArgv(argv: Array<String>) {
        var gui = false

        for (arg in argv) {
            if (arg == "--gui" || arg == "-g") {
                gui = true
            } else if (arg == "--nogui" || arg == "-G") {
                gui = false
            } else if (arg.startsWith("--port=") || arg.startsWith("-p=")) {
                ApplicationConfig.config.port = arg.split("=".toRegex(), limit = 2).toTypedArray()[1].toInt()
            } else if (arg.startsWith("--host=") || arg.startsWith("-h=")) {
                ApplicationConfig.config.host = arg.split("=".toRegex(), limit = 2).toTypedArray()[1]
            } else if (arg.startsWith("--import=") || arg.startsWith("-i=")) {
                importPath = arg.split("=".toRegex(), limit = 2).toTypedArray()[1]
            } else if (arg == "--metrics" || arg == "-m") {
                useMetrics = true
            }
        }

        if (gui) {
            createGui()
        }
    }

    private fun importFromOldVersion() {
        var sourcePath: Path? = null
        if (importPath == null) {
            // no import path given on command line
            if (Files.exists(Paths.get("config", "autosave"))) {
                logger.info("Found existing autosave dir - skipping import")
                return
            } // if not first start don't import


            var newestAutosave: Long = 0
            try {
                Files.newDirectoryStream(Paths.get(".").toAbsolutePath().normalize().parent).use { stream ->
                    for (dir in stream) {
                        if (Files.isDirectory(dir)) {
                            val autosave = dir.resolve(Paths.get("config", "autosave", "scoreboard-0-secs-ago.json"))
                            if (Files.exists(autosave) && Files.getLastModifiedTime(autosave)
                                    .toMillis() > newestAutosave
                            ) {
                                newestAutosave = Files.getLastModifiedTime(autosave).toMillis()
                                sourcePath = dir
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                logger.error("Error looking for instance to import from:", e)
                logger.info("Skipping import")
                return
            }
        } else if (importPath == "") {
            logger.info("Skipping import as per user request")
            return  // user explicitly requested no import
        } else {
            sourcePath = Paths.get(importPath!!)
        }

        if (sourcePath == null) {
            logger.info("No valid import path found - skipping import")
            return
        }

        logger.info("importing data from $sourcePath")
        val targetPath = Paths.get(".")
        try {
            copyFiles(
                sourcePath, targetPath, Paths.get("config", "autosave"), ".json",
                StandardCopyOption.REPLACE_EXISTING
            )
            copyFiles(sourcePath, targetPath, Paths.get(""), ".xlsx")
            copyDir(sourcePath, targetPath, Paths.get("config", "penalties"))
            copyDir(sourcePath, targetPath, Paths.get("html", "game-data"))
            copyDir(sourcePath, targetPath, Paths.get("html", "custom"))
            copyDir(sourcePath, targetPath, Paths.get("html", "images"))
            copyDir(sourcePath, targetPath, Paths.get("html", "videos"))
        } catch (e: IOException) {
            logger.error("Exception during importing: ", e)
        }
    }

    private fun createGui() {
        if (guiFrame != null) {
            return
        }

        guiFrame = JFrame("CRG ScoreBoard")
        guiFrame!!.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        guiMessages = JTextArea()
        guiMessages!!.isEditable = false
        guiMessages!!.font = Font("monospaced", Font.PLAIN, 12)
        guiFrameText = JLabel("ScoreBoard status: starting...")
        guiFrame!!.contentPane.layout = BoxLayout(guiFrame!!.contentPane, BoxLayout.Y_AXIS)
        guiFrame!!.contentPane.add(guiFrameText)
        guiFrame!!.contentPane.add(JScrollPane(guiMessages))
        guiFrame!!.setSize(800, 600)
        val dim = Toolkit.getDefaultToolkit().screenSize
        val w = guiFrame!!.size.width
        val h = guiFrame!!.size.height
        val x = (dim.width - w) / 2
        val y = (dim.height - h) / 2
        guiFrame!!.setLocation(x, y)
        guiFrame!!.isVisible = true
    }

    private var guiFrame: JFrame? = null
    private var guiMessages: JTextArea? = null
    private var guiFrameText: JLabel? = null

    private var importPath: String? = null

    private val logFile = File(BasePath.get(), "logs/crg.log")

    private var useMetrics = false

    init {
        logger.info("Starting Kingpin application")
        logger.info("Database type: ${ApplicationConfig.config.databaseConfig.type}")
        parseArgv(argv)
        logFile.parentFile.mkdirs()
        importFromOldVersion()
        start()
        if (guiFrameText != null) {
            guiFrameText!!.text = "ScoreBoard status: running (close this window to exit scoreboard)"
        }
    }

    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            System.setProperty(
                "org.eclipse.jetty.util.log.class",
                "org.apache.logging.log4j.appserver.jetty.Log4j2Logger"
            )
            Log4j2Logging.initialize()
            ScoreboardApplication(argv)
        }

        @Throws(IOException::class)
        private fun copyDir(src: Path, dst: Path, subdirectory: Path, vararg options: CopyOption) {
            Files.walkFileTree(src.resolve(subdirectory), object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.createDirectories(dst.resolve(src.relativize(dir)))
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    try {
                        Files.copy(file, dst.resolve(src.relativize(file)), *options)
                        return FileVisitResult.CONTINUE
                    } catch (e: FileAlreadyExistsException) {
                        return FileVisitResult.CONTINUE
                    }
                }
            })
        }

        @Throws(IOException::class)
        private fun copyFiles(src: Path, dst: Path, subdirectory: Path, suffix: String, vararg options: CopyOption) {
            Files.createDirectories(dst.resolve(subdirectory))
            val paths = Files.list(src.resolve(subdirectory))
                .map { obj: Path -> obj.normalize() }
                .filter { path: Path -> path.fileName.toString().endsWith(suffix) }
                .collect(Collectors.toList())
            for (path in paths) {
                Files.copy(path, dst.resolve(src.relativize(path)), *options)
            }
        }

        private var scoreBoard: ScoreBoard? = null
    }
}
