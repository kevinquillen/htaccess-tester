package com.github.kevinquillen.htaccess.http

import org.junit.Assert.*
import org.junit.Test

class HtaccessApiExceptionTest {

    @Test
    fun `basic exception creation`() {
        val exception = HtaccessApiException("Test error")

        assertEquals("Test error", exception.message)
        assertNull(exception.statusCode)
        assertNull(exception.cause)
        assertFalse(exception.isRetryable)
        assertFalse(exception.isRateLimited)
        assertFalse(exception.isSchemaError)
    }

    @Test
    fun `exception with status code`() {
        val exception = HtaccessApiException(
            message = "Not found",
            statusCode = 404
        )

        assertEquals("Not found", exception.message)
        assertEquals(404, exception.statusCode)
        assertFalse(exception.isRetryable)
    }

    @Test
    fun `retryable exception`() {
        val exception = HtaccessApiException(
            message = "Service unavailable",
            statusCode = 503,
            isRetryable = true
        )

        assertTrue(exception.isRetryable)
        assertEquals(503, exception.statusCode)
    }

    @Test
    fun `rate limited exception`() {
        val exception = HtaccessApiException(
            message = "Rate limit exceeded",
            statusCode = 429,
            isRateLimited = true
        )

        assertTrue(exception.isRateLimited)
        assertFalse(exception.isRetryable)
        assertEquals(429, exception.statusCode)
    }

    @Test
    fun `schema error exception`() {
        val exception = HtaccessApiException(
            message = "Response format changed",
            statusCode = 200,
            isSchemaError = true
        )

        assertTrue(exception.isSchemaError)
        assertFalse(exception.isRetryable)
    }

    @Test
    fun `exception with cause`() {
        val cause = RuntimeException("Original error")
        val exception = HtaccessApiException(
            message = "Wrapped error",
            cause = cause
        )

        assertEquals(cause, exception.cause)
        assertEquals("Wrapped error", exception.message)
    }

    @Test
    fun `all flags can be set together`() {
        val exception = HtaccessApiException(
            message = "Complex error",
            statusCode = 500,
            isRetryable = true,
            isRateLimited = false,
            isSchemaError = false
        )

        assertEquals(500, exception.statusCode)
        assertTrue(exception.isRetryable)
        assertFalse(exception.isRateLimited)
        assertFalse(exception.isSchemaError)
    }
}
