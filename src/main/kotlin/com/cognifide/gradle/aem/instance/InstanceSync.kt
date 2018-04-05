package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.Patterns
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
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
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

    val vmStatUrl = "${instance.httpUrl}/system/console/vmstat"

    fun get(url: String, configurer: (HttpRequestBase) -> Unit = { _ -> }): String {
        val method = HttpGet(normalizeUrl(url))
        configurer(method)

        return fetch(method)
    }

    fun postUrlencoded(url: String, params: Map<String, Any> = mapOf(),
                       configurer: (HttpRequestBase) -> Unit = { _ -> },
                       statuses: List<Int> = listOf(HttpStatus.SC_OK)): String {
        return post(url, createEntityUrlencoded(params), configurer, statuses)
    }

    fun postMultipart(url: String, params: Map<String, Any> = mapOf(),
                      configurer: (HttpRequestBase) -> Unit = { _ -> },
                      statuses: List<Int> = listOf(HttpStatus.SC_OK)): String {
        return post(url, createEntityMultipart(params), configurer, statuses)
    }

    private fun post(url: String, entity: HttpEntity, configurer: (HttpRequestBase) -> Unit = { _ -> },
                     statuses: List<Int> = listOf(HttpStatus.SC_OK)): String {
        val method = HttpPost(normalizeUrl(url))
        method.entity = entity
        configurer(method)

        return fetch(method, statuses)
    }

    /**
     * Fix for HttpClient's: 'escaped absolute path not valid'
     * https://stackoverflow.com/questions/13652681/httpclient-invalid-uri-escaped-absolute-path-not-valid
     */
    private fun normalizeUrl(url: String): String {
        return url.replace(" ", "%20")
    }

    fun fetch(method: HttpRequestBase, statuses: List<Int> = listOf(HttpStatus.SC_OK)): String {
        return execute(method, statuses, { IOUtils.toString(it.entity.content) })
    }

    fun <T> execute(method: HttpRequestBase, statuses: List<Int>, success: (HttpResponse) -> T): T {
        try {
            val client = createHttpClient()
            val response = client.execute(method)

            if (statuses.contains(response.statusLine.statusCode)) {
                return success(response)
            } else {
                logger.debug(IOUtils.toString(response.entity.content))
                throw DeployException("Unexpected instance response: ${response.statusLine}")
            }
        } catch (e: Exception) {
            throw DeployException("Failed instance request: ${e.message}", e)
        } finally {
            method.releaseConnection()
        }
    }

    fun createHttpClient(): HttpClient {
        val httpClientBuilder = HttpClientBuilder.create()
                .addInterceptorFirst(PreemptiveAuthInterceptor())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(config.deployConnectionTimeout)
                        .setConnectionRequestTimeout(config.deployConnectionTimeout)
                        .build()
                )
                .setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
                    setCredentials(AuthScope.ANY, UsernamePasswordCredentials(instance.user, instance.password))
                })
        if (config.trustAllCertificates) {
            httpClientBuilder.setSSLSocketFactory(createSslConnectionSocketFactory())
        }

        return httpClientBuilder.build()
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

    fun determineLocalPackage(): File {
        if (!config.localPackagePath.isBlank()) {
            val configFile = File(config.localPackagePath)
            if (configFile.exists()) {
                return configFile
            }
        }

        val archiveFile = AemConfig.pkg(project).archivePath
        if (archiveFile.exists()) {
            return archiveFile
        }

        throw DeployException("Local package not found under path: '${archiveFile.absolutePath}'. Is it built already?")
    }

    fun determineRemotePackage(): ListResponse.Package? {
        return resolveRemotePackage({ response ->
            response.resolvePackage(project, ListResponse.Package(project))
        }, true)
    }

    fun determineRemotePackagePath(): String {
        if (!config.remotePackagePath.isBlank()) {
            return config.remotePackagePath
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
        logger.debug("Asking AEM for uploaded packages using URL: '$listPackagesUrl'")

        if (instance.packages == null || refresh) {
            val json = postMultipart(listPackagesUrl)
            instance.packages = try {
                ListResponse.fromJson(json)
            } catch (e: Exception) {
                throw DeployException("Cannot ask AEM for uploaded packages!", e)
            }
        }

        return resolver(instance.packages!!)
    }

    fun uploadPackage(file: File = determineLocalPackage()): UploadResponse {
        lateinit var exception: DeployException
        for (i in 0..config.uploadRetryTimes) {
            try {
                return uploadPackageOnce(file)
            } catch (e: DeployException) {
                exception = e

                if (i < config.uploadRetryTimes) {
                    logger.warn("Cannot upload package to $instance.")
                    logger.warn("Retrying (${i+1}/${config.uploadRetryTimes}) after delay.")
                    Behaviors.waitFor(config.uploadRetryDelay)
                }
            }
        }

        throw exception
    }

    private fun uploadPackageOnce(file: File = determineLocalPackage()): UploadResponse {
        val url = "$jsonTargetUrl/?cmd=upload"

        logger.info("Uploading package at path '{}' to URL '{}'", file.path, url)

        try {
            val json = postMultipart(url, mapOf(
                    "package" to file,
                    "force" to (config.uploadForce || isSnapshot(file))
            ))
            val response = UploadResponse.fromJson(json)

            if (response.isSuccess) {
                logger.info(response.msg)
            } else {
                logger.error(response.msg)
                throw DeployException(response.msg)
            }

            return response
        } catch (e: FileNotFoundException) {
            throw DeployException(String.format("Package file '%s' not found!", file.path), e)
        } catch (e: Exception) {
            throw DeployException("Cannot upload package", e)
        }
    }

    fun installPackage(uploadedPackagePath: String = determineRemotePackagePath()): InstallResponse {
        lateinit var exception: DeployException
        for (i in 0..config.installRetryTimes) {
            try {
                return installPackageOnce(uploadedPackagePath)
            } catch (e: DeployException) {
                exception = e
                if (i < config.installRetryTimes) {
                    logger.warn("Cannot install package on $instance.")
                    logger.warn("Retrying (${i+1}/${config.installRetryTimes}) after delay.")
                    Behaviors.waitFor(config.installRetryDelay)
                }
            }
        }

        throw exception
    }

    private fun installPackageOnce(uploadedPackagePath: String): InstallResponse {
        val url = "$htmlTargetUrl$uploadedPackagePath/?cmd=install"

        logger.info("Installing package using command: $url")

        try {
            val json = postMultipart(url, mapOf(
                    "recursive" to config.installRecursive,
                    "acHandling" to config.acHandling
            ))
            val response = InstallResponse(json)

            when (response.status) {
                HtmlResponse.Status.SUCCESS -> if (response.errors.isEmpty()) {
                    logger.info("Package successfully installed.")
                } else {
                    logger.warn("Package installed with errors")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Installation completed with errors!")
                }
                HtmlResponse.Status.SUCCESS_WITH_ERRORS -> {
                    logger.error("Package installed with errors.")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Installation completed with errors!")
                }
                HtmlResponse.Status.FAIL -> {
                    logger.error("Installation failed.")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Installation incomplete!")
                }
            }

            return response
        } catch (e: Exception) {
            throw DeployException("Cannot install package.", e)
        }
    }

    fun satisfyPackage(file: File, action: () -> Unit): Boolean {
        val pkg = determineRemotePackage(file, config.satisfyRefreshing)

        return if (pkg == null) {
            action()
            true
        } else {
            if (!pkg.installed || isSnapshot(file)) {
                action()
                true
            } else {
                false
            }
        }
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, config.deploySnapshots)
    }

    fun deployPackage(file: File = determineLocalPackage()): InstallResponse {
        return installPackage(uploadPackage(file).path)
    }

    fun distributePackage(file: File = determineLocalPackage()) {
        val packagePath = uploadPackage(file).path

        installPackage(packagePath)
        activatePackage(packagePath)
    }

    fun activatePackage(path: String = determineRemotePackagePath()): UploadResponse {
        val url = jsonTargetUrl + path + "/?cmd=replicate"

        logger.info("Activating package using command: $url")

        val json: String
        try {
            json = postMultipart(url)
        } catch (e: Exception) {
            throw DeployException("Cannot activate package", e)
        }

        val response = try {
            UploadResponse.fromJson(json)
        } catch (e: Exception) {
            logger.error("Malformed JSON response", e)
            throw DeployException("Package activation failed", e)
        }

        if (response.isSuccess) {
            logger.info("Package activated")
        } else {
            logger.error("Package activation failed: + " + response.msg)
            throw DeployException(response.msg.orEmpty())
        }

        return response
    }

    fun deletePackage(path: String = determineRemotePackagePath()) {
        val url = htmlTargetUrl + path + "/?cmd=delete"

        logger.info("Deleting package using command: " + url)

        try {
            val rawHtml = postMultipart(url)
            val response = DeleteResponse(rawHtml)

            when (response.status) {
                HtmlResponse.Status.SUCCESS,
                HtmlResponse.Status.SUCCESS_WITH_ERRORS -> if (response.errors.isEmpty()) {
                    logger.info("Package successfully deleted.")
                } else {
                    logger.warn("Package deleted with errors.")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Package deleted with errors!")
                }
                HtmlResponse.Status.FAIL -> {
                    logger.error("Package deleting failed.")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Package deleting failed!")
                }
            }

        } catch (e: Exception) {
            throw DeployException("Cannot delete package.", e)
        }
    }

    fun uninstallPackage(installedPackagePath: String = determineRemotePackagePath()) {
        val url = htmlTargetUrl + installedPackagePath + "/?cmd=uninstall"

        logger.info("Uninstalling package using command: " + url)

        try {
            val rawHtml = postMultipart(url, mapOf(
                    "recursive" to config.installRecursive,
                    "acHandling" to config.acHandling
            ))
            val response = UninstallResponse(rawHtml)

            when (response.status) {
                HtmlResponse.Status.SUCCESS,
                HtmlResponse.Status.SUCCESS_WITH_ERRORS -> if (response.errors.isEmpty()) {
                    logger.info("Package successfully uninstalled.")
                } else {
                    logger.warn("Package uninstalled with errors.")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Package uninstalled with errors!")
                }
                HtmlResponse.Status.FAIL -> {
                    logger.error("Package uninstalling failed.")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Package uninstalling failed!")
                }
            }

        } catch (e: Exception) {
            throw DeployException("Cannot uninstall package.", e)
        }
    }

    fun determineBundleState(configurer: (HttpRequestBase) -> Unit = { _ -> }): BundleState {
        logger.debug("Asking AEM for bundles using URL: '$bundlesUrl'")

        return try {
            BundleState.fromJson(get(bundlesUrl, configurer))
        } catch (e: Exception) {
            logger.debug("Cannot determine bundle state on $instance", e)
            BundleState.unknown(e)
        }
    }

    fun reload() {
        try {
            logger.info("Triggering instance(s) shutdown")
            postUrlencoded(vmStatUrl, mapOf("shutdown_type" to "Restart"))

            logger.info("Awaiting instance(s) shutdown")
            Behaviors.waitFor(config.reloadDelay)
        } catch (e: DeployException) {
            throw InstanceException("Cannot reload instance $instance", e)
        }
    }

}