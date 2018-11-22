package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.Retry
import com.cognifide.gradle.aem.internal.BuildScope
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.ProgressCountdown
import com.cognifide.gradle.aem.internal.file.FileException
import com.cognifide.gradle.aem.internal.file.downloader.HttpFileDownloader
import com.cognifide.gradle.aem.internal.http.RequestException
import com.cognifide.gradle.aem.internal.http.ResponseException
import com.cognifide.gradle.aem.pkg.*
import com.cognifide.gradle.aem.pkg.resolver.PackageException
import java.io.File
import java.io.FileNotFoundException
import org.gradle.api.Project
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.zeroturnaround.zip.ZipUtil

class InstanceSync(project: Project, instance: Instance) : InstanceHttpClient(project, instance) {

    fun determineRemotePackagePath(file: File): String {
        val pkg = determineRemotePackage(file)
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
        aem.logger.debug("Asking for uploaded packages on $instance")

        val packages = BuildScope.of(project).getOrPut("instance.${instance.name}.packages", {
            try {
                postMultipart(PKG_MANAGER_LIST_JSON) { ListResponse.fromJson(asStream(it)) }
            } catch (e: Exception) {
                throw DeployException("Cannot ask for uploaded packages on $instance.", e)
            }
        }, refresh)

        return resolver(packages)
    }

    fun uploadPackage(file: File) = uploadPackage(file, true, Retry.once())

    fun uploadPackage(file: File, force: Boolean, retry: Retry): UploadResponse {
        lateinit var exception: DeployException
        for (i in 0..retry.times) {
            try {
                return uploadPackageOnce(file, force)
            } catch (e: DeployException) {
                exception = e

                if (i < retry.times) {
                    aem.logger.warn("Cannot upload package $file to $instance.")
                    aem.logger.debug("Upload error", e)

                    val header = "Retrying upload (${i + 1}/${retry.times}) after delay."
                    val countdown = ProgressCountdown(project, header, retry.delay(i + 1))
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun uploadPackageOnce(file: File, force: Boolean): UploadResponse {
        val url = "$PKG_MANAGER_JSON_PATH/?cmd=upload"

        aem.logger.info("Uploading package $file to $instance'")

        val response = try {
            postMultipart(url, mapOf(
                    "package" to file,
                    "force" to (force || isSnapshot(file))
            )) { UploadResponse.fromJson(asStream(it)) }
        } catch (e: FileNotFoundException) {
            throw DeployException("Package file $file to be uploaded not found!", e)
        } catch (e: RequestException) {
            throw DeployException("Cannot upload package $file to $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw DeployException("Malformed response after uploading package $file to $instance.", e)
        }

        if (!response.isSuccess) {
            throw DeployException("Cannot upload package $file to $instance. Reason: ${response.msg}.")
        }

        return response
    }

    fun downloadPackage(remotePath: String, targetFile: File, retry: Retry = Retry.once()) {
        lateinit var exception: FileException
        val url = instance.httpUrl + remotePath

        for (i in 0..retry.times) {
            try {
                downloadPackageOnce(url, targetFile)
                return
            } catch (e: FileException) {
                exception = e

                if (i < retry.times) {
                    aem.logger.warn("Cannot download package $remotePath from $instance.")
                    aem.logger.debug("Download error", e)

                    val header = "Retrying download (${i + 1}/${retry.times}) after delay."
                    val countdown = ProgressCountdown(project, header, retry.delay(i + 1))
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun downloadPackageOnce(url: String, targetFile: File) {
        aem.logger.info("Downloading package from $url to file $targetFile")

        with(HttpFileDownloader(project)) {
            username = basicUser
            password = basicPassword
            preemptiveAuthentication = true

            download(url, targetFile)
        }

        if (!targetFile.exists()) {
            throw FileException("Downloaded package missing: ${targetFile.path}")
        }
    }

    fun buildPackage(remotePath: String): PackageBuildResponse {
        val url = "$PKG_MANAGER_JSON_PATH$remotePath/?cmd=build"

        aem.logger.info("Building package $remotePath on $instance")

        val response = try {
            postMultipart(url) { PackageBuildResponse.fromJson(asStream(it)) }
        } catch (e: RequestException) {
            throw DeployException("Cannot build package $remotePath on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw DeployException("Malformed response after building package $remotePath on $instance.", e)
        }

        if (!response.isSuccess) {
            throw DeployException("Cannot build package $remotePath on $instance. Reason: ${response.msg}.")
        }
        return response
    }

    fun installPackage(remotePath: String, recursive: Boolean = true, retry: Retry = Retry.once()): InstallResponse {
        lateinit var exception: DeployException
        for (i in 0..retry.times) {
            try {
                return installPackageOnce(remotePath, recursive)
            } catch (e: DeployException) {
                exception = e
                if (i < retry.times) {
                    aem.logger.warn("Cannot install package $remotePath on $instance.")
                    aem.logger.debug("Install error", e)

                    val header = "Retrying install (${i + 1}/${retry.times}) after delay."
                    val countdown = ProgressCountdown(project, header, retry.delay(i + 1))
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun installPackageOnce(remotePath: String, recursive: Boolean = true): InstallResponse {
        val url = "$PKG_MANAGER_HTML_PATH$remotePath/?cmd=install"

        aem.logger.info("Installing package $remotePath on $instance")

        val response = try {
            postMultipart(url, mapOf("recursive" to recursive)) { InstallResponse.from(asStream(it), aem.config.packageResponseBuffer) }
        } catch (e: RequestException) {
            throw DeployException("Cannot install package $remotePath on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw DeployException("Malformed response after installing package $remotePath on $instance.")
        }

        val packageErrors = response.findPackageErrors(aem.config.packageErrors)
        if (packageErrors.isNotEmpty()) {
            throw PackageException("Cannot install package $remotePath on $instance because it is malformed by:\n$packageErrors \nErrors: ${response.errors}")
        } else if (!response.success) {
            throw DeployException("Cannot install package $remotePath on $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, aem.config.packageSnapshots)
    }

    fun deployPackage(
        file: File,
        uploadForce: Boolean = true,
        uploadRetry: Retry = Retry.once(),
        installRecursive: Boolean = true,
        installRetry: Retry = Retry.once()
    ) {
        val uploadResponse = uploadPackage(file, uploadForce, uploadRetry)
        installPackage(uploadResponse.path, installRecursive, installRetry)
    }

    fun distributePackage(
        file: File,
        uploadForce: Boolean = true,
        uploadRetry: Retry = Retry.once(),
        installRecursive: Boolean = true,
        installRetry: Retry = Retry.once()
    ) {
        val uploadResponse = uploadPackage(file, uploadForce, uploadRetry)
        val packagePath = uploadResponse.path

        installPackage(packagePath, installRecursive, installRetry)
        activatePackage(packagePath)
    }

    fun activatePackage(remotePath: String): UploadResponse {
        val url = "$PKG_MANAGER_JSON_PATH$remotePath/?cmd=replicate"

        aem.logger.info("Activating package $remotePath on $instance")

        val response = try {
            postMultipart(url) { UploadResponse.fromJson(asStream(it)) }
        } catch (e: RequestException) {
            throw DeployException("Cannot activate package $remotePath on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw DeployException("Malformed response after activating package $remotePath on $instance.", e)
        }

        if (!response.isSuccess) {
            throw DeployException("Cannot activate package $remotePath on $instance. Reason: ${response.msg}.")
        }

        return response
    }

    fun deletePackage(remotePath: String): DeleteResponse {
        val url = "$PKG_MANAGER_HTML_PATH$remotePath/?cmd=delete"

        aem.logger.info("Deleting package $remotePath on $instance")

        val response = try {
            postMultipart(url) { DeleteResponse.from(asStream(it), aem.config.packageResponseBuffer) }
        } catch (e: RequestException) {
            throw DeployException("Cannot delete package $remotePath from $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw DeployException("Malformed response after deleting package $remotePath from $instance.", e)
        }

        if (!response.success) {
            throw DeployException("Cannot delete package $remotePath from $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun uninstallPackage(remotePath: String): UninstallResponse {
        val url = "$PKG_MANAGER_HTML_PATH$remotePath/?cmd=uninstall"

        aem.logger.info("Uninstalling package using command: $url")

        val response = try {
            postMultipart(url) { UninstallResponse.from(asStream(it), aem.config.packageResponseBuffer) }
        } catch (e: RequestException) {
            throw DeployException("Cannot uninstall package $remotePath on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw DeployException("Malformed response after uninstalling package $remotePath from $instance.", e)
        }

        if (!response.success) {
            throw DeployException("Cannot uninstall package $remotePath from $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun determineInstanceState(): InstanceState {
        return InstanceState(this, instance)
    }

    fun determineBundleState(): BundleState {
        aem.logger.debug("Asking for OSGi bundles on $instance")

        return try {
            get(OSGI_BUNDLES_PATH) { BundleState.from(asStream(it)) }
        } catch (e: Exception) {
            aem.logger.debug("Cannot determine OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }
    }

    fun determineComponentState(): ComponentState {
        aem.logger.debug("Asking for OSGi components on $instance")

        return try {
            get(OSGI_COMPONENTS_PATH) { ComponentState.from(asStream(it)) }
        } catch (e: Exception) {
            aem.logger.debug("Cannot determine OSGi components state on $instance", e)
            ComponentState.unknown()
        }
    }

    fun reload() {
        shutdown(OSGI_VMSTAT_SHUTDOWN_RESTART)
    }

    fun stop() {
        shutdown(OSGI_VMSTAT_SHUTDOWN_STOP)
    }

    private fun shutdown(type: String) {
        try {
            aem.logger.info("Triggering shutdown of $instance.")
            postUrlencoded(OSGI_VMSTAT_PATH, mapOf("shutdown_type" to type))
        } catch (e: DeployException) {
            throw InstanceException("Cannot trigger shutdown of $instance.", e)
        }
    }

    companion object {
        const val PKG_MANAGER_PATH = "/crx/packmgr/service"

        const val PKG_MANAGER_JSON_PATH = "$PKG_MANAGER_PATH/.json"

        const val PKG_MANAGER_HTML_PATH = "$PKG_MANAGER_PATH/.html"

        const val PKG_MANAGER_LIST_JSON = "/crx/packmgr/list.jsp"

        const val OSGI_BUNDLES_PATH = "/system/console/bundles.json"

        const val OSGI_COMPONENTS_PATH = "/system/console/components.json"

        const val OSGI_VMSTAT_PATH = "/system/console/vmstat"

        const val OSGI_VMSTAT_SHUTDOWN_STOP = "Stop"

        const val OSGI_VMSTAT_SHUTDOWN_RESTART = "Restart"
    }
}