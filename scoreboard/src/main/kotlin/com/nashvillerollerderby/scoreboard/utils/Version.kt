package com.nashvillerollerderby.scoreboard.utils

import java.io.IOException
import java.util.*

object Version {
    private val logger = Log4j2Logging.getLogger(this)

    @Throws(IOException::class)
    fun load(): Boolean {
        val versionProperties = Properties()
        val cL = Version::class.java.classLoader
        val releaseIs = cL.getResourceAsStream(VERSION_RELEASE_PROPERTIES_NAME)
        try {
            versionProperties.load(releaseIs)
        } catch (npE: NullPointerException) {
            logger.warn(
                "Could not find version release properties file '$VERSION_RELEASE_PROPERTIES_NAME'"
            )
            return false
        } catch (ioE: IOException) {
            logger.warn(
                "Could not load version release properties file '$VERSION_RELEASE_PROPERTIES_NAME'"
            )
            throw ioE
        }
        try {
            releaseIs!!.close()
        } catch (e: Exception) {
            logger.error(e)
        }
        val m: MutableMap<String, String> = HashMap()
        m[VERSION_RELEASE_KEY] = "unset"
        for (k in versionProperties.stringPropertyNames()) {
            m[k] = versionProperties.getProperty(k)
        }
        all = Collections.unmodifiableMap(m)
        logger.info("CRG ScoreBoard version " + get())
        return true
    }

    fun get(): String? {
        return all[VERSION_RELEASE_KEY]
    }

    @JvmStatic
    var all: Map<String, String> = HashMap()
        private set

    private const val VERSION_PATH: String = "com/nashvillerollerderby/scoreboard/version"
    private const val VERSION_RELEASE_PROPERTIES_NAME: String = "$VERSION_PATH/release.properties"
    private const val VERSION_RELEASE_KEY: String = "release"
}
