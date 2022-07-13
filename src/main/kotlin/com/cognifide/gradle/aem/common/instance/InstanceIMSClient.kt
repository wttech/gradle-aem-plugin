package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.ims.Secret
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.json.JSONObject
import java.io.*
import java.security.KeyFactory
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.spec.KeySpec
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Generates Bearer token based on the credentials fetched from AEMaaCS
 * by communicating with Adobe Identity Management Services.
 */
class InstanceIMSClient(private val aem: AemExtension) {

    private val common = aem.common

    /**
     * URI pointing to key file copied from AEMaaCS console.
     */
    val keyPath = aem.obj.string {
        aem.prop.string("instance.default.keyPath")?.let { set(it) }
    }

    /**
     * Directory storing the fetched secret file from AEMaaCS instance.
     */
    private val secretDir = aem.obj.dir {
        convention(aem.obj.buildDir("instance/secret"))
    }

    private val serviceTokenFile: File? get() = keyPath.orNull?.let {
        common.fileTransfer.downloadTo(it, secretDir.get().asFile)
    }

    private lateinit var secret: Secret

    private val expirationTime = System.currentTimeMillis() / 1000 + 86400L

    @Suppress("TooGenericExceptionCaught")
    fun generateToken(): String? {
        if (keyPath.isPresent) {
            try {
                readProperties()
                val jwtToken = generateJWTToken()
                return fetchAccessToken(jwtToken)
            } catch (e: Exception) {
                println("Couldn't generate the access token")
                println(e.message)
                println("Consider checking the provided file")
            }
        }
        return null
    }

    private fun readProperties() {
        if (serviceTokenFile == null) {
            throw InstanceException("No URI to the secret file is specified")
        }
        if (!File(serviceTokenFile!!.toURI()).exists()) {
            throw InstanceException("The secret file doesn't exist")
        }
        val jsonString = File(serviceTokenFile!!.toURI())
        secret = jacksonObjectMapper().readValue(jsonString.readBytes(), Secret::class.java)
    }

    private fun generateJWTToken(): String {
        val privateKeyContent: ByteArray
        PEMParser(StringReader(secret.integration.privateKey)).use { pemParser ->
            val keyPair: KeyPair = JcaPEMKeyConverter().getKeyPair(pemParser.readObject() as PEMKeyPair)
            privateKeyContent = keyPair.private.encoded
        }
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec: KeySpec = PKCS8EncodedKeySpec(privateKeyContent)
        val rsaPrivateKey = keyFactory.generatePrivate(keySpec) as RSAPrivateKey

        val imsHost = secret.integration.imsHost
        val metaScopes = secret.integration.metascopes.split(",")

        val jwtClaims = mutableMapOf<String, Any>(
            "iss" to secret.integration.orgId,
            "sub" to secret.integration.technicalAccountId,
            "exp" to expirationTime,
            "aud" to "https://$imsHost/c/${secret.integration.technicalAccount.clientId}",
        )
        val scopes = metaScopes.associate {
            "https://$imsHost/s/$it" to true
        }
        jwtClaims.putAll(scopes)

        return Jwts.builder()
            .setClaims(jwtClaims)
            .signWith(SignatureAlgorithm.RS256, rsaPrivateKey)
            .compact()
    }

    private fun fetchAccessToken(jwtToken: String): String? {
        val imsExchangeEndpoint = "https://${secret.integration.imsHost}/ims/exchange/jwt"
        val clientId = secret.integration.technicalAccount.clientId
        val clientSecret = secret.integration.technicalAccount.clientSecret

        val params = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "jwt_token" to jwtToken
        )

        val response = common.http {
            post(imsExchangeEndpoint, params) { httpResponse ->
                asStream(httpResponse).bufferedReader().use { it.readText() }
            }
        }

        return JSONObject(response).getString("access_token")
    }
}
