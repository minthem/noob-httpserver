package io.github.minthem.noobhttpserver.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpStatusTest {

    @Test
    fun `test isInformational returns true for informational status codes`() {
        val status = HttpStatus.CONTINUE
        assertTrue(status.isInformational())
        assertFalse(status.isSuccess())
        assertFalse(status.isRedirection())
        assertFalse(status.isClientError())
        assertFalse(status.isServerError())
    }

    @Test
    fun `test isSuccess returns true for success status codes`() {
        val status = HttpStatus.OK
        assertTrue(status.isSuccess())
        assertFalse(status.isInformational())
        assertFalse(status.isRedirection())
        assertFalse(status.isClientError())
        assertFalse(status.isServerError())
    }

    @Test
    fun `test isRedirection returns true for redirection status codes`() {
        val status = HttpStatus.MOVED_PERMANENTLY
        assertTrue(status.isRedirection())
        assertFalse(status.isInformational())
        assertFalse(status.isSuccess())
        assertFalse(status.isClientError())
        assertFalse(status.isServerError())
    }

    @Test
    fun `test isClientError returns true for client error status codes`() {
        val status = HttpStatus.NOT_FOUND
        assertTrue(status.isClientError())
        assertFalse(status.isInformational())
        assertFalse(status.isSuccess())
        assertFalse(status.isRedirection())
        assertFalse(status.isServerError())
    }

    @Test
    fun `test isServerError returns true for server error status codes`() {
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        assertTrue(status.isServerError())
        assertFalse(status.isInformational())
        assertFalse(status.isSuccess())
        assertFalse(status.isRedirection())
        assertFalse(status.isClientError())
    }

    @Test
    fun `test isError returns true for both client and server errors`() {
        val clientError = HttpStatus.NOT_FOUND
        val serverError = HttpStatus.INTERNAL_SERVER_ERROR

        assertTrue(clientError.isError())
        assertTrue(serverError.isError())
        assertFalse(clientError.isInformational())
        assertFalse(serverError.isInformational())
    }

    @Test
    fun `test isError returns false for non error statuses`() {
        assertFalse(HttpStatus.OK.isError())
        assertFalse(HttpStatus.CONTINUE.isError())
        assertFalse(HttpStatus.MOVED_PERMANENTLY.isError())
    }

    @Test
    fun `test status exposes code and reason phrase`() {
        assertEquals(200, HttpStatus.OK.code)
        assertEquals("OK", HttpStatus.OK.reasonPhrase)
        assertEquals(404, HttpStatus.NOT_FOUND.code)
        assertEquals("Not Found", HttpStatus.NOT_FOUND.reasonPhrase)
    }

    @Test
    fun `test boundary classifications are correct`() {
        assertTrue(HttpStatus.EARLY_HINTS.isInformational())
        assertTrue(HttpStatus.IM_USED.isSuccess())
        assertTrue(HttpStatus.PERMANENT_REDIRECT.isRedirection())
        assertTrue(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.isClientError())
        assertTrue(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED.isServerError())
    }

    @Test
    fun `test toString formats code and reason phrase correctly`() {
        val status = HttpStatus.IM_A_TEAPOT
        assertEquals("418 I'm a teapot", status.toString())
    }
}