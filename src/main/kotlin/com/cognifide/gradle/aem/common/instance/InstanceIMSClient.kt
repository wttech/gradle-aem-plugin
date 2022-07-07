package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.gradle.internal.impldep.com.google.api.client.http.HttpMethods
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.security.KeyFactory
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.spec.KeySpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.HttpsURLConnection

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

    private lateinit var privateKey: String

    private lateinit var orgId: String

    private lateinit var technicalAccountId: String

    private lateinit var clientId: String

    private lateinit var clientSecret: String

    private lateinit var imsHost: String

    private lateinit var imsExchangeEndpoint: String

    private val expirationTime = System.currentTimeMillis() / 1000 + 86400L

    private lateinit var metaScopes: List<String>

    private fun readProperties() {
        if (serviceTokenFile == null) {
            throw InstanceException("No secret file available for generating access token")
        }
        val jsonString = Files.readString(serviceTokenFile!!.toPath())
        val obj = JSONObject(jsonString)
        val integration = obj.getJSONObject("integration")

        privateKey = integration.getString("privateKey")
        orgId = integration.getString("org")
        technicalAccountId = integration.getString("id")
        clientId = integration.getJSONObject("technicalAccount").getString("clientId")
        clientSecret = integration.getJSONObject("technicalAccount").getString("clientSecret")
        imsHost = integration.getString("imsEndpoint")
        imsExchangeEndpoint = "https://$imsHost/ims/exchange/jwt"
        metaScopes = integration.getString("metascopes").split(",")
    }

    private fun generateJWTToken(): String {
        var privateKeyContent: ByteArray
        PEMParser(StringReader(privateKey)).use { pemParser ->
            val keyPair: KeyPair = JcaPEMKeyConverter().getKeyPair(pemParser.readObject() as PEMKeyPair)
            privateKeyContent = keyPair.private.encoded
        }
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec: KeySpec = PKCS8EncodedKeySpec(privateKeyContent)
        val rsaPrivateKey = keyFactory.generatePrivate(keySpec) as RSAPrivateKey

        val jwtClaims = mutableMapOf<String, Any>(
            "iss" to orgId,
            "sub" to technicalAccountId,
            "exp" to expirationTime,
            "aud" to "https://$imsHost/c/$clientId",
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
        val connection = URL(imsExchangeEndpoint).openConnection() as HttpsURLConnection
        connection.requestMethod = HttpMethods.POST
        val urlParameters = "client_id=$clientId&client_secret=$clientSecret&jwt_token=$jwtToken"

        // Send post request
        connection.doOutput = true
        val writer = DataOutputStream(connection.outputStream)
        writer.writeBytes(urlParameters)
        writer.flush()
        writer.close()

        var responseError = false

        val inputStream: InputStream
        if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
            inputStream = connection.inputStream
        } else {
            inputStream = connection.errorStream
            responseError = true
        }

        val response = inputStream.bufferedReader().use { it.readText() }

        if (responseError) {
            println(response)
            return null
        }

        return JSONObject(response).getString("access_token")
    }

    companion object {

        @Suppress("TooGenericExceptionCaught")
        fun generateBearerTokenOrNull(aem: AemExtension): String? {
            val client = InstanceIMSClient(aem)
            with(client) {
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
        }
    }
}
