package net.ardevd.tagius.core.network

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for LoginRetrofitClient URL parsing logic.
 * 
 * These tests verify that the determineApiPath function correctly identifies
 * timetagger.app URLs regardless of protocol or subdomain variations, and
 * properly defaults to self-hosted paths for all other URLs.
 */
class LoginRetrofitClientTest {

    @Test
    fun determineApiPath_httpsTimetaggerApp_returnsHostedPath() {
        // Given: Official timetagger.app URL with HTTPS
        val baseUrl = "https://timetagger.app"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_httpTimetaggerApp_returnsHostedPath() {
        // Given: Official timetagger.app URL with HTTP (no SSL)
        val baseUrl = "http://timetagger.app"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_httpsWwwTimetaggerApp_returnsHostedPath() {
        // Given: Official timetagger.app URL with www subdomain and HTTPS
        val baseUrl = "https://www.timetagger.app"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path (www should be stripped)
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_httpWwwTimetaggerApp_returnsHostedPath() {
        // Given: Official timetagger.app URL with www subdomain and HTTP
        val baseUrl = "http://www.timetagger.app"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path (www should be stripped)
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_timetaggerAppWithTrailingSlash_returnsHostedPath() {
        // Given: Official timetagger.app URL with trailing slash
        val baseUrl = "https://timetagger.app/"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_timetaggerAppWithPath_returnsHostedPath() {
        // Given: Official timetagger.app URL with path
        val baseUrl = "https://timetagger.app/some/path"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path (path doesn't affect host detection)
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_selfHostedHttpsUrl_returnsSelfHostedPath() {
        // Given: Self-hosted TimeTagger URL with HTTPS
        val baseUrl = "https://my-server.com"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_selfHostedHttpUrl_returnsSelfHostedPath() {
        // Given: Self-hosted TimeTagger URL with HTTP
        val baseUrl = "http://my-server.com"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_selfHostedWithPort_returnsSelfHostedPath() {
        // Given: Self-hosted TimeTagger URL with port
        val baseUrl = "https://my-server.com:8080"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_selfHostedIpAddress_returnsSelfHostedPath() {
        // Given: Self-hosted TimeTagger URL with IP address
        val baseUrl = "http://192.168.1.100"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_localhostUrl_returnsSelfHostedPath() {
        // Given: Localhost URL for local development
        val baseUrl = "http://localhost:8000"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_similarDomain_returnsSelfHostedPath() {
        // Given: URL that is similar but not exactly timetagger.app
        val baseUrl = "https://timetagger.app.example.com"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path (not exact match)
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_subdomainOfTimetaggerApp_returnsSelfHostedPath() {
        // Given: Subdomain of timetagger.app (not www)
        val baseUrl = "https://api.timetagger.app"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path (only www subdomain is stripped)
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_malformedUrl_returnsSelfHostedPath() {
        // Given: Malformed URL that cannot be parsed
        val baseUrl = "not-a-valid-url"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path (safe fallback)
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_emptyString_returnsSelfHostedPath() {
        // Given: Empty URL string
        val baseUrl = ""
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted API path (safe fallback)
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_urlWithUsernamePassword_timetaggerApp_returnsHostedPath() {
        // Given: timetagger.app URL with username/password (rare but valid URL format)
        val baseUrl = "https://user:pass@timetagger.app"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path (host detection ignores credentials)
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_wwwPrefixInDomain_notStripped() {
        // Given: Domain that legitimately starts with 'www' as part of domain name
        val baseUrl = "https://wwwtimetagger.com"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return self-hosted path (removePrefix only removes "www." not "www")
        assertEquals("timetagger/api/v2/", result)
    }

    @Test
    fun determineApiPath_caseVariations_handledCorrectly() {
        // Given: timetagger.app with different case (URLs are case-insensitive for host)
        // Note: URL class normalizes host to lowercase automatically
        val baseUrl = "https://TimeTagger.App"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path (URL normalization handles case)
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_timetaggerAppWithQuery_returnsHostedPath() {
        // Given: timetagger.app URL with query parameters
        val baseUrl = "https://timetagger.app?param=value"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path (query params don't affect host)
        assertEquals("api/v2/", result)
    }

    @Test
    fun determineApiPath_timetaggerAppWithFragment_returnsHostedPath() {
        // Given: timetagger.app URL with fragment
        val baseUrl = "https://timetagger.app#section"
        
        // When: Determining API path
        val result = LoginRetrofitClient.determineApiPath(baseUrl)
        
        // Then: Should return hosted API path (fragments don't affect host)
        assertEquals("api/v2/", result)
    }
}
