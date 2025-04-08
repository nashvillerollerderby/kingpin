package com.nashvillerollerderby.scoreboard

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule

enum class DatabaseType {
    JSON,
    SQLITE,
    POSTGRESQL,
}

data class DatabaseConfig(
    val type: DatabaseType = DatabaseType.JSON
)

@ConsistentCopyVisibility
data class ApplicationConfig private constructor(
    var port: Int = 8000,
    var host: String? = null,
    val databaseConfig: DatabaseConfig = DatabaseConfig(),
) {
    companion object {
        val config: ApplicationConfig by lazy {
            val classLoader = Thread.currentThread().contextClassLoader
            val yaml = classLoader.getResource("kingpin.yaml")!!
            val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
            mapper.readValue(yaml, ApplicationConfig::class.java)
        }
    }
}
