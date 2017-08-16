package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPackagePlugin
import com.cognifide.gradle.aem.deploy.*
import com.cognifide.gradle.aem.pkg.ComposeTask
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.multipart.*
import org.apache.commons.httpclient.params.HttpConnectionParams
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.FileNotFoundException

class InstanceSync(val project: Project, val instance: Instance) {

    companion object {
        private val PACKAGE_MANAGER_SERVICE_SUFFIX = "/crx/packmgr/service"

        private val PACKAGE_MANAGER_LIST_SUFFIX = "/crx/packmgr/list.jsp"
    }

    val config = AemConfig.of(project)

    val logger = project.logger

    val jsonTargetUrl = instance.httpUrl + PACKAGE_MANAGER_SERVICE_SUFFIX + "/.json"

    val htmlTargetUrl = instance.httpUrl + PACKAGE_MANAGER_SERVICE_SUFFIX + "/.html"

    val listPackagesUrl = instance.httpUrl + PACKAGE_MANAGER_LIST_SUFFIX

    val bundlesUrl = "${instance.httpUrl}/system/console/bundles.json"

    fun get(url: String, parametrizer: (HttpConnectionParams) -> Unit = {}): String {
        val method = GetMethod(url)

        return execute(method, parametrizer)
    }

    fun post(url: String, params: Map<String, Any> = mapOf(), parametrizer: (HttpConnectionParams) -> Unit = {}): String {
        val method = PostMethod(url)
        method.requestEntity = MultipartRequestEntity(createParts(params).toTypedArray(), method.params)

        return execute(method, parametrizer)
    }

    fun execute(method: HttpMethod, parametrizer: (HttpConnectionParams) -> Unit = {}): String {
        try {
            val client = createHttpClient()
            parametrizer(client.httpConnectionManager.params)

            val status = client.executeMethod(method)
            if (status == HttpStatus.SC_OK) {
                return IOUtils.toString(method.responseBodyAsStream)
            } else {
                logger.debug(method.responseBodyAsString)
                throw DeployException("Request to the instance failed, cause: "
                        + HttpStatus.getStatusText(status) + " (check URL, user and password)")
            }

        } catch (e: Exception) {
            throw DeployException("Request to the instance failed, cause: " + e.message, e)
        } finally {
            method.releaseConnection()
        }
    }

    fun createHttpClient(): HttpClient {
        val client = HttpClient()
        client.httpConnectionManager.params.connectionTimeout = config.deployConnectionTimeout
        client.httpConnectionManager.params.soTimeout = config.deployConnectionTimeout
        client.params.isAuthenticationPreemptive = true
        client.state.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(instance.user, instance.password))

        return client
    }

    private fun createParts(params: Map<String, Any>): List<Part> {
        val partList = mutableListOf<Part>()
        for ((key, value) in params) {
            if (value is File) {
                val file = value
                try {
                    partList.add(FilePart(key, FilePartSource(file.name, file)))
                } catch (e: FileNotFoundException) {
                    throw DeployException(String.format("Upload param '%s' has invalid file specified.", key), e)
                }
            } else {
                val str = value.toString()
                if (!str.isNullOrBlank()) {
                    partList.add(StringPart(key, str))
                }
            }
        }

        return partList
    }

    fun determineLocalPackage(): File {
        if (!config.localPackagePath.isNullOrBlank()) {
            val configFile = File(config.localPackagePath)
            if (configFile.exists()) {
                return configFile
            }
        }

        val archiveFile = (project.tasks.getByName(ComposeTask.NAME) as ComposeTask).archivePath
        if (archiveFile.exists()) {
            return archiveFile
        }

        throw DeployException("Local package not found under path: '${archiveFile.absolutePath}'. Is it built already?")
    }

    fun determineRemotePackagePath(): String {
        if (!config.remotePackagePath.isNullOrBlank()) {
            return config.remotePackagePath
        }

        val pkg = resolveRemotePackage({ response ->
            response.resolvePackage(project, ListResponse.Package(project))
        }) ?: throw DeployException("Package is not uploaded on AEM instance.")

        logger.info("Package found on AEM at path: '${pkg.path}'")

        return pkg.path
    }

    fun determineRemotePackage(file: File): ListResponse.Package? {
        val xml = ZipUtil.unpackEntry(file, AemPackagePlugin.VLT_PROPERTIES).toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())

        val group = doc.select("entry[key=group]").text()
        val name = doc.select("entry[key=name]").text()
        val version = doc.select("entry[key=version]").text()

        return resolveRemotePackage({ response ->
            response.resolvePackage(project, ListResponse.Package(group, name, version))
        })
    }

    private fun resolveRemotePackage(resolver: (ListResponse) -> ListResponse.Package?): ListResponse.Package? {
        logger.info("Asking AEM for uploaded packages using URL: '$listPackagesUrl'")

        val json = post(listPackagesUrl)
        val response = try {
            ListResponse.fromJson(json)
        } catch (e: Exception) {
            throw DeployException("Cannot ask AEM for uploaded packages!", e)
        }

        return resolver(response)
    }

    fun uploadPackage(file: File = determineLocalPackage()): UploadResponse {
        val sync = InstanceSync(project, instance)
        val url = sync.jsonTargetUrl + "/?cmd=upload"

        logger.info("Uploading package at path '{}' to URL '{}'", file.path, url)

        try {
            val json = sync.post(url, mapOf(
                    "package" to file,
                    "force" to config.uploadForce
            ))
            val response = UploadResponse.fromJson(json)

            if (response.isSuccess) {
                logger.info(response.msg)
            } else {
                logger.error(response.msg)
                throw DeployException(response.msg.orEmpty())
            }

            return response
        } catch (e: FileNotFoundException) {
            throw DeployException(String.format("Package file '%s' not found!", file.path), e)
        } catch (e: Exception) {
            throw DeployException("Cannot upload package", e)
        }
    }

    fun installPackage(uploadedPackagePath: String = determineRemotePackagePath()): InstallResponse {
        val url = htmlTargetUrl + uploadedPackagePath + "/?cmd=install"

        logger.info("Installing package using command: " + url)

        try {
            val json = post(url, mapOf(
                    "recursive" to config.recursiveInstall,
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

    fun satisfyPackage(file: File): Boolean {
        val pkg = determineRemotePackage(file)

        return if (pkg == null) {
            deployPackage(file)
            true
        } else {
            if (pkg.installed) {
                false
            } else {
                deployPackage(file)
                true
            }
        }
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

        logger.info("Activating package using command: " + url)

        val json: String
        try {
            json = post(url)
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
            val rawHtml = post(url)
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
            val rawHtml = post(url, mapOf(
                    "recursive" to config.recursiveInstall,
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

    fun determineBundleState(parametrizer: (HttpConnectionParams) -> Unit): BundleState {
        return try {
            BundleState.fromJson(get(bundlesUrl, parametrizer))
        } catch (e: Exception) {
            logger.debug("Cannot determine bundle state on $instance", e)
            BundleState.unknown(e)
        }
    }

}