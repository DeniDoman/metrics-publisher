package com.domanskii.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import mu.KotlinLogging
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.security.PrivateKey
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Handles GitHub App authentication with JWT generation and installation token management.
 * 
 * Based on Spinnaker's GitHubAppAuthenticator implementation.
 * 
 * Features:
 * - JWT generation for GitHub App authentication
 * - Installation token management with automatic refresh
 * - Thread-safe token caching
 * - Support for both PKCS#1 and PKCS#8 PEM formats
 */
class GitHubAppAuth(
    private val appId: String,
    privateKeyPath: String,
    private val installationId: String,
    private val baseUrl: String = "https://api.github.com"
) {
    private val log = KotlinLogging.logger {}
    private val privateKey: PrivateKey = loadPrivateKey(privateKeyPath)
    
    @Volatile
    private var cachedToken: CachedToken? = null

    companion object {
        private const val JWT_EXPIRATION_MINUTES = 10L
        private const val TOKEN_REFRESH_BUFFER_MINUTES = 5L
    }

    /**
     * Returns an authenticated GitHub client using installation token.
     * 
     * The returned GitHub client does NOT automatically refresh tokens.
     * Installation tokens expire after 1 hour. Call this method for each
     * operation rather than caching the client long-term.
     * 
     * @return Authenticated GitHub client with a currently-valid token
     */
    fun getAuthenticatedClient(): GitHub {
        val token = getInstallationToken()
        return GitHubBuilder()
            .withEndpoint(baseUrl)
            .withAppInstallationToken(token)
            .build()
    }

    /**
     * Gets a valid installation token, using cache when possible.
     * 
     * @return Valid installation token
     */
    fun getInstallationToken(): String {
        val current = cachedToken
        if (current == null || isTokenExpired(current)) {
            synchronized(this) {
                val recheck = cachedToken
                if (recheck == null || isTokenExpired(recheck)) {
                    cachedToken = fetchInstallationToken()
                }
            }
        }
        return cachedToken!!.token
    }

    private fun fetchInstallationToken(): CachedToken {
        val jwt = generateJWT()
        
        // Use GitHub API to get installation token
        val gitHubApp = GitHubBuilder()
            .withEndpoint(baseUrl)
            .withJwtToken(jwt)
            .build()
        
        val installation = gitHubApp.app.getInstallationById(installationId.toLong())
        val token = installation.createToken().create()
        
        log.debug { 
            "Successfully obtained GitHub App installation token for app $appId " +
            "installation $installationId, expires at: ${token.expiresAt}"
        }
        
        return CachedToken(token.token, token.expiresAt.toInstant())
    }

    private fun generateJWT(): String {
        val now = Instant.now()
        val issuedAt = Date.from(now)
        val expiresAt = Date.from(now.plusSeconds(TimeUnit.MINUTES.toSeconds(JWT_EXPIRATION_MINUTES)))
        
        return Jwts.builder()
            .setIssuer(appId)
            .setIssuedAt(issuedAt)
            .setExpiration(expiresAt)
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact()
    }
    
    private fun isTokenExpired(token: CachedToken?): Boolean {
        if (token?.expiresAt == null) return true
        
        val refreshThreshold = Instant.now()
            .plusSeconds(TimeUnit.MINUTES.toSeconds(TOKEN_REFRESH_BUFFER_MINUTES))
        return token.expiresAt.isBefore(refreshThreshold)
    }

    /**
     * Loads a private key from a PEM file.
     * Supports both PKCS#1 (RSA PRIVATE KEY) and PKCS#8 (PRIVATE KEY) formats.
     * 
     * @param privateKeyPath Path to the PEM file
     * @return Loaded private key
     */
    private fun loadPrivateKey(privateKeyPath: String): PrivateKey {
        try {
            Files.newBufferedReader(Paths.get(privateKeyPath)).use { keyReader ->
                PEMParser(keyReader).use { pemParser ->
                    val obj = pemParser.readObject()
                    val converter = JcaPEMKeyConverter()
                    
                    return when (obj) {
                        is PEMKeyPair -> {
                            // For PKCS#1 format (-----BEGIN RSA PRIVATE KEY-----)
                            converter.getPrivateKey(obj.privateKeyInfo)
                        }
                        is PrivateKeyInfo -> {
                            // For PKCS#8 format (-----BEGIN PRIVATE KEY-----)
                            converter.getPrivateKey(obj)
                        }
                        else -> {
                            throw IllegalArgumentException(
                                "Unsupported PEM format. Expected RSA private key, got: ${obj?.javaClass?.name}"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load GitHub App private key from: $privateKeyPath", e)
        }
    }

    /**
     * Cached token with expiration time
     */
    private data class CachedToken(
        val token: String,
        val expiresAt: Instant
    )
}
