package io.github.minthem.noob.logging

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.event.Level
import java.util.concurrent.ConcurrentHashMap

class SimpleLoggerFactory : ILoggerFactory {
    private val loggers = ConcurrentHashMap<String, Logger>()

    private val defaultLevel: Level =
        try {
            val levelStr = System.getenv("NOOB_LOG_LEVEL") ?: "INFO"
            Level.valueOf(levelStr.uppercase())
        } catch (e: Exception) {
            Level.INFO
        }

    override fun getLogger(name: String): Logger? = loggers.getOrPut(name) { SimpleLogger(name, defaultLevel) }
}
