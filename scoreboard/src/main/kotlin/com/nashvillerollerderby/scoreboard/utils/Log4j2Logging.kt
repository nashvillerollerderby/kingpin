package com.nashvillerollerderby.scoreboard.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.yaml.YamlConfiguration

object Log4j2Logging {
    fun initialize() {
        val classLoader = Thread.currentThread().contextClassLoader
        val yml = classLoader.getResourceAsStream("log4j2.yaml")!!
        val source = ConfigurationSource(yml)
        Configurator.initialize(YamlConfiguration(null, source))
    }

    @JvmStatic
    fun <T : Any> getLogger(cClass: T): Logger {
        return LogManager.getLogger(cClass::class.java)
    }
}
