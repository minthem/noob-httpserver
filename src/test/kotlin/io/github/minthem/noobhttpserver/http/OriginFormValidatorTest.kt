package io.github.minthem.noobhttpserver.http

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the OriginFormValidator object, which validates 
 * URI targets in the origin-form as defined in RFC 9110.
 * 
 * Tests cover various valid and invalid cases for both the path 
 * and query components of a target.
 */
internal class OriginFormValidatorTest {

    @Test
    fun `isValid returns true for a valid path with no query`() {
        val target = "/valid/path"
        val result = OriginFormValidator.isValid(target)
        assertTrue(result)
    }

    @Test
    fun `isValid returns true for a valid path and query`() {
        val target = "/valid/path?param1=value1&param2=value2"
        val result = OriginFormValidator.isValid(target)
        assertTrue(result)
    }

    @Test
    fun `isValid returns true for an empty path and query`() {
        val target = ""
        val result = OriginFormValidator.isValid(target)
        assertTrue(result)
    }

    @Test
    fun `isValid returns false for a path without leading slash`() {
        val target = "invalid/path"
        val result = OriginFormValidator.isValid(target)
        assertFalse(result)
    }

    @Test
    fun `isValid returns false for a path with an invalid segment`() {
        val target = "/invalid/path/segment^"
        val result = OriginFormValidator.isValid(target)
        assertFalse(result)
    }

    @Test
    fun `isValid returns true for a path with valid percent-encoded characters`() {
        val target = "/valid/%20path"
        val result = OriginFormValidator.isValid(target)
        assertTrue(result)
    }

    @Test
    fun `isValid returns false for a path with invalid percent-encoded sequence`() {
        val target = "/invalid/%XZpath"
        val result = OriginFormValidator.isValid(target)
        assertFalse(result)
    }

    @Test
    fun `isValid returns true for a query with reserved characters`() {
        val target = "/path?param1=value1&param2=value2!$&'()*+,;="
        val result = OriginFormValidator.isValid(target)
        assertTrue(result)
    }

    @Test
    fun `isValid returns false for a query with invalid percent-encoded sequence`() {
        val target = "/path?param=%ZXvalue"
        val result = OriginFormValidator.isValid(target)
        assertFalse(result)
    }

    @Test
    fun `isValid returns true for a query with valid percent-encoded characters`() {
        val target = "/path?param=%20value"
        val result = OriginFormValidator.isValid(target)
        assertTrue(result)
    }

    @Test
    fun `isValid returns false for entirely invalid target`() {
        val target = "invalid/%path?query^"
        val result = OriginFormValidator.isValid(target)
        assertFalse(result)
    }

    @Test
    fun `isValid returns true for a valid path with trailing slash`() {
        val target = "/valid/path/"
        val result = OriginFormValidator.isValid(target)
        assertTrue(result)
    }

    @Test
    fun `isValid returns false for a path with invalid character`() {
        val target = "/path/with/invalid|char"
        val result = OriginFormValidator.isValid(target)
        assertFalse(result)
    }

    @Test
    fun `isValid returns true for a query containing question marks`() {
        val target = "/path?first=1?second=2"
        val result = OriginFormValidator.isValid(target)
        assertTrue(result)
    }
}