package io.github.minthem.noob.http.config

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigTest {
    @Nested
    inner class ServerConfigTest {
        @Test
        fun `creates server config with default values`() {
            val actual = ServerConfig()

            assertEquals(8080u, actual.port)
        }

        @Test
        fun `throws when port is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    ServerConfig(port = 0u)
                }

            assertEquals("Port must be positive and within valid range", actual.message)
        }
    }

    @Nested
    inner class TimeoutConfigTest {
        @Test
        fun `creates timeout config with default values`() {
            val actual = TimeoutConfig()

            assertEquals(30_000, actual.readMillis)
            assertEquals(30_000, actual.writeMillis)
            assertEquals(120_000, actual.sessionMillis)
            assertEquals(30_000, actual.shutdownMillis)
        }

        @Test
        fun `throws when read timeout is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    TimeoutConfig(readMillis = 0)
                }

            assertEquals("Read timeout must be positive", actual.message)
        }

        @Test
        fun `throws when write timeout is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    TimeoutConfig(writeMillis = 0)
                }

            assertEquals("Write timeout must be positive", actual.message)
        }

        @Test
        fun `throws when session timeout is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    TimeoutConfig(sessionMillis = 0)
                }

            assertEquals("Session timeout must be positive", actual.message)
        }

        @Test
        fun `throws when shutdown timeout is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    TimeoutConfig(shutdownMillis = 0)
                }

            assertEquals("Shutdown timeout must be positive", actual.message)
        }

        @Test
        fun `throws when read timeout is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    TimeoutConfig(readMillis = -1)
                }

            assertEquals("Read timeout must be positive", actual.message)
        }

        @Test
        fun `throws when write timeout is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    TimeoutConfig(writeMillis = -1)
                }

            assertEquals("Write timeout must be positive", actual.message)
        }

        @Test
        fun `throws when session timeout is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    TimeoutConfig(sessionMillis = -1)
                }

            assertEquals("Session timeout must be positive", actual.message)
        }

        @Test
        fun `throws when shutdown timeout is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    TimeoutConfig(shutdownMillis = -1)
                }

            assertEquals("Shutdown timeout must be positive", actual.message)
        }
    }

    @Nested
    inner class BufferConfigTest {
        @Test
        fun `creates buffer config with default values`() {
            val actual = BufferConfig()

            assertEquals(8 * 1024, actual.requestBytes)
            assertEquals(2 * 1024, actual.responseHeaderBytes)
        }

        @Test
        fun `throws when request buffer size is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    BufferConfig(requestBytes = 0)
                }

            assertEquals("Request buffer size must be positive", actual.message)
        }

        @Test
        fun `throws when response header buffer size is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    BufferConfig(responseHeaderBytes = 0)
                }

            assertEquals("Response header buffer size must be positive", actual.message)
        }

        @Test
        fun `throws when request buffer size is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    BufferConfig(requestBytes = -1)
                }

            assertEquals("Request buffer size must be positive", actual.message)
        }

        @Test
        fun `throws when response header buffer size is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    BufferConfig(responseHeaderBytes = -1)
                }

            assertEquals("Response header buffer size must be positive", actual.message)
        }
    }

    @Nested
    inner class HttpLimitsConfigTest {
        @Test
        fun `creates http limits config with default values`() {
            val actual = HttpLimitsConfig()

            assertEquals(8 * 1024, actual.maxRequestTargetBytes)
            assertEquals(16 * 1024, actual.maxHeaderSectionBytes)
            assertEquals(256, actual.maxHeaderNameBytes)
            assertEquals(8 * 1024, actual.maxHeaderValueBytes)
            assertEquals(100, actual.maxHeaderCount)
            assertEquals(16 * 1024 * 1024, actual.maxRequestBodyBytes)
            assertEquals(8 * 1024 * 1024, actual.maxChunkSizeBytes)
        }

        @Test
        fun `throws when max request line bytes is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxRequestTargetBytes = 0)
                }

            assertEquals("Max request target bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max header section bytes is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxHeaderSectionBytes = 0)
                }

            assertEquals("Max header section bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max header name bytes is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxHeaderNameBytes = 0)
                }

            assertEquals("Max header name bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max header value bytes is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxHeaderValueBytes = 0)
                }

            assertEquals("Max header value bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max header count is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxHeaderCount = 0)
                }

            assertEquals("Max header count must be positive", actual.message)
        }

        @Test
        fun `throws when max request body bytes is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxRequestBodyBytes = 0)
                }

            assertEquals("Max request body bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max chunk size bytes is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxChunkSizeBytes = 0)
                }

            assertEquals("Max chunk size bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max request line bytes is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxRequestTargetBytes = -1)
                }

            assertEquals("Max request target bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max header section bytes is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxHeaderSectionBytes = -1)
                }

            assertEquals("Max header section bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max header name bytes is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxHeaderNameBytes = -1)
                }

            assertEquals("Max header name bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max header value bytes is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxHeaderValueBytes = -1)
                }

            assertEquals("Max header value bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max header count is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxHeaderCount = -1)
                }

            assertEquals("Max header count must be positive", actual.message)
        }

        @Test
        fun `throws when max request body bytes is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxRequestBodyBytes = -1)
                }

            assertEquals("Max request body bytes must be positive", actual.message)
        }

        @Test
        fun `throws when max chunk size bytes is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    HttpLimitsConfig(maxChunkSizeBytes = -1)
                }

            assertEquals("Max chunk size bytes must be positive", actual.message)
        }
    }

    @Nested
    inner class MultipartConfigTest {
        @Test
        fun `creates multipart config with default values`() {
            val actual = MultipartConfig()

            assertEquals(1 * 1024 * 1024, actual.memoryThresholdBytes)
        }

        @Test
        fun `throws when memory threshold is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    MultipartConfig(memoryThresholdBytes = 0)
                }

            assertEquals("Memory threshold must be positive", actual.message)
        }

        @Test
        fun `throws when memory threshold is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    MultipartConfig(memoryThresholdBytes = -1)
                }

            assertEquals("Memory threshold must be positive", actual.message)
        }
    }

    @Nested
    inner class KeepAliveConfigTest {
        @Test
        fun `creates keep alive config with default values`() {
            val actual = KeepAliveConfig()

            assertEquals(true, actual.enabled)
            assertEquals(3000L, actual.idleTimeoutMillis)
            assertEquals(100, actual.maxRequests)
        }

        @Test
        fun `throws when idle timeout is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    KeepAliveConfig(idleTimeoutMillis = 0)
                }

            assertEquals("Idle timeout must be positive", actual.message)
        }

        @Test
        fun `throws when idle timeout is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    KeepAliveConfig(idleTimeoutMillis = -1)
                }

            assertEquals("Idle timeout must be positive", actual.message)
        }

        @Test
        fun `throws when max requests is zero`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    KeepAliveConfig(maxRequests = 0)
                }

            assertEquals("Max requests must be positive", actual.message)
        }

        @Test
        fun `throws when max requests is negative`() {
            val actual =
                assertFailsWith<IllegalArgumentException> {
                    KeepAliveConfig(maxRequests = -1)
                }

            assertEquals("Max requests must be positive", actual.message)
        }
    }
}
