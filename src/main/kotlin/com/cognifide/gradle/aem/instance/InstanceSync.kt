package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.ProgressCountdown
import com.cognifide.gradle.aem.internal.http.PreemptiveAuthInterceptor
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.deploy.*
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.ssl.SSLContextBuilder
import org.gradle.api.Project
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class InstanceSync(val project: Project, val instance: Instance) {

    companion object {
        private const val PACKAGE_MANAGER_SERVICE_SUFFIX = "/crx/packmgr/service"

        private const val PACKAGE_MANAGER_LIST_SUFFIX = "/crx/packmgr/list.jsp"
    }

    val config = AemConfig.of(project)

    val logger = project.logger

    val jsonTargetUrl = instance.httpUrl + PACKAGE_MANAGER_SERVICE_SUFFIX + "/.json"

    val htmlTargetUrl = instance.httpUrl + PACKAGE_MANAGER_SERVICE_SUFFIX + "/.html"

    val listPackagesUrl = instance.httpUrl + PACKAGE_MANAGER_LIST_SUFFIX

    val bundlesUrl = "${instance.httpUrl}/system/console/bundles.json"

    val componentsUrl = "${instance.httpUrl}/system/console/components.json"

    val vmStatUrl = "${instance.httpUrl}/system/console/vmstat"

    var basicUser = instance.user

    var basicPassword = instance.password

    var connectionTimeout = config.instanceConnectionTimeout

    var connectionUntrustedSsl = config.instanceConnectionUntrustedSsl

    var connectionRetries = true

    var requestConfigurer: (HttpRequestBase) -> Unit = { _ -> }

    var responseHandler: (HttpResponse) -> Unit = { _ -> }

    fun get(url: String): String {
        return fetch(HttpGet(normalizeUrl(url)))
    }

    fun head(url: String): String {
        return fetch(HttpHead(normalizeUrl(url)))
    }

    fun delete(url: String): String {
        return fetch(HttpDelete(normalizeUrl(url)))
    }

    fun put(url: String): String {
        return fetch(HttpPut(normalizeUrl(url)))
    }

    fun patch(url: String): String {
        return fetch(HttpPatch(normalizeUrl(url)))
    }

    fun postUrlencoded(url: String, params: Map<String, Any> = mapOf()): String {
        return post(url, createEntityUrlencoded(params))
    }

    fun postMultipart(url: String, params: Map<String, Any> = mapOf()): String {
        return post(url, createEntityMultipart(params))
    }

    private fun post(url: String, entity: HttpEntity): String {
        return fetch(HttpPost(normalizeUrl(url)).apply { this.entity = entity })
    }

    /**
     * Fix for HttpClient's: 'escaped absolute path not valid'
     * https://stackoverflow.com/questions/13652681/httpclient-invalid-uri-escaped-absolute-path-not-valid
     */
    private fun normalizeUrl(url: String): String {
        return url.replace(" ", "%20")
    }

    fun fetch(method: HttpRequestBase): String {
        return execute(method, { response ->
            val body = IOUtils.toString(response.entity.content) ?: ""

            if (response.statusLine.statusCode == HttpStatus.SC_OK) {
                return@execute body
            } else {
                logger.debug(body)
                throw DeployException("Unexpected response from $instance: ${response.statusLine}")
            }
        })
    }

    fun <T> execute(method: HttpRequestBase, success: (HttpResponse) -> T): T {
        try {
            requestConfigurer(method)

            val client = createHttpClient()
            val response = client.execute(method)

            responseHandler(response)

            return success(response)
        } catch (e: Exception) {
            throw DeployException("Failed request to $instance: ${e.message}", e)
        } finally {
            method.releaseConnection()
        }
    }

    fun createHttpClient(): HttpClient {
        val builder = HttpClientBuilder.create()
                .addInterceptorFirst(PreemptiveAuthInterceptor())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(connectionTimeout)
                        .setConnectionRequestTimeout(connectionTimeout)
                        .build()
                )
                .setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
                    setCredentials(AuthScope.ANY, UsernamePasswordCredentials(basicUser, basicPassword))
                })
        if (connectionUntrustedSsl) {
            builder.setSSLSocketFactory(createSslConnectionSocketFactory())
        }
        if (!connectionRetries) {
            builder.disableAutomaticRetries()
        }

        return builder.build()
    }

    private fun createSslConnectionSocketFactory(): SSLConnectionSocketFactory {
        val sslContext = SSLContextBuilder()
                .loadTrustMaterial(null, { _, _ -> true })
                .build()
        return SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)
    }

    private fun createEntityUrlencoded(params: Map<String, Any>): HttpEntity {
        return UrlEncodedFormEntity(params.entries.fold(ArrayList<NameValuePair>(), { result, e ->
            result.add(BasicNameValuePair(e.key, e.value.toString())); result
        }))
    }

    private fun createEntityMultipart(params: Map<String, Any>): HttpEntity {
        val builder = MultipartEntityBuilder.create()
        for ((key, value) in params) {
            if (value is File) {
                if (value.exists()) {
                    builder.addBinaryBody(key, value)
                }
            } else {
                val str = value.toString()
                if (str.isNotBlank()) {
                    builder.addTextBody(key, str)
                }
            }
        }

        return builder.build()
    }

    fun determineRemotePackage(): ListResponse.Package? {
        return resolveRemotePackage({ response ->
            response.resolvePackage(project, ListResponse.Package(project))
        }, true)
    }

    fun determineRemotePackagePath(): String {
        if (!config.packageRemotePath.isBlank()) {
            return config.packageRemotePath
        }

        val pkg = determineRemotePackage()
                ?: throw DeployException("Package is not uploaded on AEM instance.")

        return pkg.path
    }

    fun determineRemotePackage(file: File, refresh: Boolean = true): ListResponse.Package? {
        if (!ZipUtil.containsEntry(file, PackagePlugin.VLT_PROPERTIES)) {
            throw DeployException("File is not a valid CRX package: $file")
        }

        val xml = ZipUtil.unpackEntry(file, PackagePlugin.VLT_PROPERTIES).toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())

        val group = doc.select("entry[key=group]").text()
        val name = doc.select("entry[key=name]").text()
        val version = doc.select("entry[key=version]").text()

        return resolveRemotePackage({ response ->
            response.resolvePackage(project, ListResponse.Package(group, name, version))
        }, refresh)
    }

    private fun resolveRemotePackage(resolver: (ListResponse) -> ListResponse.Package?, refresh: Boolean): ListResponse.Package? {
        logger.debug("Asking for uploaded packages using URL: '$listPackagesUrl'")

        if (instance.packages == null || refresh) {
            val json = postMultipart(listPackagesUrl)
            instance.packages = try {
                ListResponse.fromJson(json)
            } catch (e: Exception) {
                throw DeployException("Cannot ask for uploaded packages on $instance.", e)
            }
        }

        return resolver(instance.packages!!)
    }

    fun uploadPackage(file: File): UploadResponse {
        lateinit var exception: DeployException
        for (i in 0..config.uploadRetryTimes) {
            try {
                return uploadPackageOnce(file)
            } catch (e: DeployException) {
                exception = e

                if (i < config.uploadRetryTimes) {
                    logger.warn("Cannot upload package $file to $instance.")
                    logger.debug("Upload error", e)

                    val header = "Retrying upload (${i + 1}/${config.uploadRetryTimes}) after delay."
                    val countdown = ProgressCountdown(project, header, config.uploadRetryDelay)
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun uploadPackageOnce(file: File): UploadResponse {
        val url = "$jsonTargetUrl/?cmd=upload"

        logger.info("Uploading package at path '{}' using URL '{}'", file.path, url)

        val json = try {
            postMultipart(url, mapOf(
                    "package" to file,
                    "force" to (config.uploadForce || isSnapshot(file))
            ))
        } catch (e: FileNotFoundException) {
            throw DeployException("Package file $file to be uploaded not found!", e)
        } catch (e: Exception) {
            throw DeployException("Cannot upload package $file to instance $instance. Reason: request failed.", e)
        }

        try {
            val response = UploadResponse.fromJson(json)
            if (!response.isSuccess) {
                throw DeployException("Cannot upload package $file to instance $instance. Reason: ${response.msg}.")
            }

            return response
        } catch (e: Exception) {
            throw DeployException("Malformed response after uploading package $file to instance $instance.", e)
        }
    }

    fun installPackage(remotePath: String): InstallResponse {
        lateinit var exception: DeployException
        for (i in 0..config.installRetryTimes) {
            try {
                return installPackageOnce(remotePath)
            } catch (e: DeployException) {
                exception = e
                if (i < config.installRetryTimes) {
                    logger.warn("Cannot install package $remotePath on $instance.")
                    logger.debug("Install error", e)

                    val header = "Retrying install (${i + 1}/${config.installRetryTimes}) after delay."
                    val countdown = ProgressCountdown(project, header, config.installRetryDelay)
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun installPackageOnce(remotePath: String): InstallResponse {
        val url = "$htmlTargetUrl$remotePath/?cmd=install"

        logger.info("Installing package using command: $url")

        val json = try {
            postMultipart(url, mapOf("recursive" to config.installRecursive))
        } catch (e: Exception) {
            throw DeployException("Cannot install package $remotePath on instance $instance. Reason: request failed.", e)
        }

        try {
            val response = InstallResponse(json)
            if (!response.success) {
                throw DeployException("Cannot install package $remotePath on instance $instance. Status: ${response.status}. Errors: ${response.errors}.")
            }

            return response
        } catch (e: Exception) {
            throw DeployException("Malformed install response after installing package $remotePath on instance $instance.", e)
        }
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, config.packageSnapshots)
    }

    fun deployPackage(file: File) {
        installPackage(uploadPackage(file).path)
    }

    fun distributePackage(file: File) {
        val packagePath = uploadPackage(file).path

        installPackage(packagePath)
        activatePackage(packagePath)
    }

    fun activatePackage(remotePath: String): UploadResponse {
        val url = "$jsonTargetUrl$remotePath/?cmd=replicate"

        logger.info("Activating package using command: $url")

        val json = try {
            postMultipart(url)
        } catch (e: Exception) {
            throw DeployException("Cannot activate package $remotePath on instance $instance. Reason: request failed.", e)
        }

        try {
            val response = UploadResponse.fromJson(json)
            if (!response.isSuccess) {
                throw DeployException("Cannot activate package $remotePath on instance $instance. Reason: ${response.msg}.")
            }

            return response
        } catch (e: Exception) {
            throw DeployException("Malformed response after activating package $remotePath on instance $instance.", e)
        }
    }

    fun deletePackage(remotePath: String) {
        val url = "$htmlTargetUrl$remotePath/?cmd=delete"

        logger.info("Deleting package using command: $url")

        val rawHtml = try {
           postMultipart(url)
        } catch (e: Exception) {
            throw DeployException("Cannot delete package $remotePath from instance $instance. Reason: request failed.", e)
        }

        try {
            val response = DeleteResponse(rawHtml)
            if (!response.success) {
                throw DeployException("Cannot delete package $remotePath from instance $instance. Status: ${response.status}. Errors: ${response.errors}.")
            }
        } catch (e: Exception) {
            throw DeployException("Malformed response after deleting package $remotePath from instance $instance.", e)
        }
    }

    fun uninstallPackage(remotePath: String) {
        val url = "$htmlTargetUrl$remotePath/?cmd=uninstall"

        logger.info("Uninstalling package using command: $url")

        val rawHtml = try {
            postMultipart(url, mapOf("recursive" to config.installRecursive))
        } catch (e: Exception) {
            throw DeployException("Cannot uninstall package $remotePath on instance $instance. Reason: request failed.", e)
        }

        try {
            val response = UninstallResponse(rawHtml)
            if (!response.success) {
                throw DeployException("Cannot uninstall package $remotePath from $instance. Status: ${response.status}. Errors: ${response.errors}.")
            }
        } catch (e: Exception) {
            throw DeployException("Malformed response after uninstalling package $remotePath from instance $instance.", e)
        }
    }

    fun determineInstanceState(): InstanceState {
        return InstanceState(this, instance)
    }

    fun determineBundleState(): BundleState {
        logger.debug("Asking for OSGi bundles using URL: '$bundlesUrl'")

        return try {
            BundleState.fromJson(get(bundlesUrl))
        } catch (e: Exception) {
            logger.debug("Cannot determine OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }
    }

    fun determineComponentState(): ComponentState {
        logger.debug("Asking for OSGi components using URL: '$bundlesUrl'")

        return try {
            ComponentState.fromJson(get(componentsUrl))
        } catch (e: Exception) {
            logger.debug("Cannot determine OSGi components state on $instance", e)
            ComponentState.unknown()
        }
    }

    fun reload() {
        try {
            logger.info("Triggering shutdown of $instance.")
            postUrlencoded(vmStatUrl, mapOf("shutdown_type" to "Restart"))
        } catch (e: DeployException) {
            throw InstanceException("Cannot trigger shutdown of $instance.", e)
        }
    }

}

fun Collection<Instance>.sync(project: Project, callback: (InstanceSync) -> Unit) {
    return map { InstanceSync(project, it) }.parallelStream().forEach(callback)
}