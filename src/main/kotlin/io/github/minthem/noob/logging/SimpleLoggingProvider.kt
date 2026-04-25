package io.github.minthem.noob.logging

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMDCAdapter
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class SimpleLoggingProvider : SLF4JServiceProvider {
    private val loggerFactory: ILoggerFactory = SimpleLoggerFactory()
    private val markerFactory: IMarkerFactory = BasicMarkerFactory()
    private val mdcAdapter: MDCAdapter = BasicMDCAdapter()

    override fun getLoggerFactory() = loggerFactory

    override fun getMarkerFactory() = markerFactory

    override fun getMDCAdapter() = mdcAdapter

    override fun getRequestedApiVersion() = "2.0.17"

    override fun initialize() {}
}
