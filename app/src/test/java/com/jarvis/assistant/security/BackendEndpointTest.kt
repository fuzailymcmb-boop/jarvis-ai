package com.jarvis.assistant.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackendEndpointTest {
    @Test fun acceptsHttpsForPhysicalPhones() {
        assertEquals("example.com", BackendEndpoint.parse("https://example.com/api/chat", false).host)
    }

    @Test fun permitsOnlyTheDebugEmulatorException() {
        assertEquals(8787, BackendEndpoint.parse("http://10.0.2.2:8787/api/chat", true).port)
        listOf("http://10.0.2.2/api/chat", "http://10.0.2.2:8080/api/chat", "http://example.com/api/chat")
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) { BackendEndpoint.parse(value, true) }
            }
        assertThrows(IllegalArgumentException::class.java) {
            BackendEndpoint.parse("http://10.0.2.2:8787/api/chat", false)
        }
    }

    @Test fun rejectsEmbeddedCredentialsAndMalformedDestinations() {
        listOf("https://user:password@example.com/api/chat", "https:///api/chat",
            "https://example.com/api/chat#fragment", "https://example.com:99999/api/chat")
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) { BackendEndpoint.parse(value, false) }
            }
    }
}
