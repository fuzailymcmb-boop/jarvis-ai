package com.jarvis.assistant.security

import java.net.URI
import java.net.URL

/** Validate the destination before a saved token can be sent to it. */
object BackendEndpoint {
    fun parse(value: String, allowEmulatorHttp: Boolean): URL {
        val uri = URI(value)
        val host = uri.host
        require(!host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawFragment == null) {
            "Use a server URL without embedded credentials or a fragment."
        }
        require(uri.port == -1 || uri.port in 1..65535) { "Invalid server port." }
        val https = uri.scheme.equals("https", ignoreCase = true)
        val emulator = allowEmulatorHttp && uri.scheme.equals("http", ignoreCase = true) &&
            host == "10.0.2.2" && uri.port == 8787
        require(https || emulator) {
            "Use HTTPS, or http://10.0.2.2:8787/api/chat in the debug Android emulator."
        }
        return uri.toURL()
    }
}
