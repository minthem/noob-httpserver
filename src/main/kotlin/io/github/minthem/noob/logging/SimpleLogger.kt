package io.github.minthem.noob.logging

import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SimpleLogger(
    name: String,
    private var level: Level = Level.INFO,
) : LegacyAbstractLogger() {
    init {
        this.name = name
    }

    override fun isTraceEnabled(): Boolean = isLevelEnabled(Level.TRACE)

    override fun isDebugEnabled(): Boolean = isLevelEnabled(Level.DEBUG)

    override fun isInfoEnabled(): Boolean = isLevelEnabled(Level.INFO)

    override fun isWarnEnabled(): Boolean = isLevelEnabled(Level.WARN)

    override fun isErrorEnabled(): Boolean = isLevelEnabled(Level.ERROR)

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: Level?,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?,
    ) {
        val fmtMsg =
            if (arguments != null && messagePattern != null) {
                MessageFormatter.arrayFormat(messagePattern, arguments).message
            } else {
                messagePattern ?: ""
            }

        val threadName = Thread.currentThread().name
        val fmtDatetime = formatter.format(ZonedDateTime.now())

        val outputBuilder = StringBuilder()
        outputBuilder.append(fmtDatetime)
        outputBuilder.append(" [${level?.name}] ")
        outputBuilder.append(" [$threadName] ")
        outputBuilder.append(name)
        outputBuilder.append(" - ")
        outputBuilder.append(fmtMsg)
        if (throwable != null) {
            outputBuilder.append("\n")
            outputBuilder.append(throwable.stackTraceToString())
        }

        println(outputBuilder.toString())
    }

    private fun isLevelEnabled(level: Level): Boolean = level.toInt() >= this.level.toInt()

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }
}
