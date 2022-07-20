package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.ims.AccessObject
import com.cognifide.gradle.aem.common.instance.service.ims.Secret
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.*
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Generates Bearer token based on the credentials fetched from AEMaaCS
 * by communicating with Adobe Identity Management Services.
 */
class Ims(private val aem: AemExtension) {

    private val common = aem.common

    private lateinit var secret: Secret

    private val expirationTime = aem.obj.long {
        convention(86400L)
        aem.prop.long("ims.expirationTime")?.let { set(it) }
    }

    /**
     * This is how Adobe calculates expTime in an example they provided for generating token.
     */
    private val expTime get() = System.currentTimeMillis() / 1000 + expirationTime.get()

    @Suppress("TooGenericExceptionCaught")
    fun generateToken(serviceCredentials: File): String {
        try {
            secret = readCredentialsFile(serviceCredentials)
            val jwtToken = generateJWTToken()
            val accessObject = fetchAccessObject(jwtToken)
            return accessObject.accessToken
        } catch (e: Exception) {
            throw ImsException("Could not generate the access token, consider checking the provided secret file", e)
        }
    }

    private fun readCredentialsFile(serviceCredentials: File): Secret = serviceCredentials.inputStream().use {
        common.formats.toObjectFromJson(it, Secret::class.java)
    }

    private fun generateJWTToken(): String {
        val privateKeyContent = PEMParser(StringReader(secret.integration.privateKey)).use { pemParser ->
            val keyPair = JcaPEMKeyConverter().getKeyPair(pemParser.readObject() as PEMKeyPair)
            keyPair.private.encoded
        }
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(privateKeyContent)
        val rsaPrivateKey = keyFactory.generatePrivate(keySpec)

        val imsHost = secret.integration.imsHost
        val metaScopes = secret.integration.metascopes.split(",")

        val jwtClaims = mapOf<String, Any>(
            "iss" to secret.integration.orgId,
            "sub" to secret.integration.technicalAccountId,
            "exp" to expTime,
            "aud" to "https://$imsHost/c/${secret.integration.technicalAccount.clientId}",
        ) + metaScopes.associate {
            "https://$imsHost/s/$it" to true
        }

        return Jwts.builder()
            .setClaims(jwtClaims)
            .signWith(SignatureAlgorithm.RS256, rsaPrivateKey)
            .compact()
    }

    private fun fetchAccessObject(jwtToken: String): AccessObject {
        val imsExchangeEndpoint = "https://${secret.integration.imsHost}/ims/exchange/jwt"
        val clientId = secret.integration.technicalAccount.clientId
        val clientSecret = secret.integration.technicalAccount.clientSecret

        val response = common.http {
            post(
                imsExchangeEndpoint,
                mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "jwt_token" to jwtToken
                )
            ) { httpResponse ->
                asStream(httpResponse).bufferedReader().use { it.readText() }
            }
        }

        return common.formats.toObjectFromJson(response, AccessObject::class.java)
    }
}
