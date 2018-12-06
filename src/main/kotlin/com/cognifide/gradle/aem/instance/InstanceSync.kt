package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.*
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.pkg.*
import com.cognifide.gradle.aem.pkg.tasks.Compose
import java.io.File
import java.io.FileNotFoundException
import org.gradle.api.Project
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.zeroturnaround.zip.ZipUtil

@Suppress("LargeClass", "TooManyFunctions")
class InstanceSync(project: Project, instance: Instance) : InstanceHttpClient(project, instance) {

    fun determineRemotePackage(file: File, refresh: Boolean = true): Package? {
        if (!ZipUtil.containsEntry(file, Package.VLT_PROPERTIES)) {
            throw PackageException("File is not a valid CRX package: $file")
        }

        val xml = ZipUtil.unpackEntry(file, Package.VLT_PROPERTIES).toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())

        val group = doc.select("entry[key=group]").text()
        val name = doc.select("entry[key=name]").text()
        val version = doc.select("entry[key=version]").text()

        return determineRemotePackage(group, name, version, refresh)
    }

    fun determineRemotePackage(compose: Compose, refresh: Boolean = true): Package? {
        return resolveRemotePackage({ it.resolvePackage(project, Package(compose)) }, refresh)
    }

    fun determineRemotePackage(group: String, name: String, version: String, refresh: Boolean = true): Package? {
        return resolveRemotePackage({ it.resolvePackage(project, Package(group, name, version)) }, refresh)
    }

    private fun resolveRemotePackage(resolver: (ListResponse) -> Package?, refresh: Boolean): Package? {
        aem.logger.debug("Asking for uploaded packages on $instance")

        val packages = BuildScope.of(project).getOrPut("instance.${instance.name}.packages", {
            try {
                postMultipart(PKG_MANAGER_LIST_JSON) { asObjectFromJson(it, ListResponse::class.java) }
            } catch (e: AemException) {
                throw InstanceException("Cannot ask for uploaded packages on $instance.", e)
            }
        }, refresh)

        return resolver(packages)
    }

    fun determineRemotePackagePath(file: File, refresh: Boolean = true): String {
        return getPackagePathOrFail(determineRemotePackage(file, refresh))
    }

    fun determineRemotePackagePath(compose: Compose, refresh: Boolean = true): String {
        return getPackagePathOrFail(determineRemotePackage(compose, refresh))
    }

    private fun getPackagePathOrFail(pkg: Package?): String {
        return pkg?.path ?: throw InstanceException("Package is not uploaded on AEM instance.")
    }

    fun uploadPackage(file: File) = uploadPackage(file, true, Retry.none())

    fun uploadPackage(file: File, force: Boolean, retry: Retry): UploadResponse {
        lateinit var exception: InstanceException
        for (i in 0..retry.times) {
            try {
                return uploadPackageOnce(file, force)
            } catch (e: InstanceException) {
                exception = e

                if (i < retry.times) {
                    aem.logger.warn("Cannot upload package $file to $instance.")
                    aem.logger.debug("Upload error", e)

                    val delay = retry.delay(i + 1)
                    val countdown = ProgressCountdown(project, delay)

                    aem.logger.lifecycle("Retrying upload (${i + 1}/${retry.times}) after delay: ${Formats.duration(delay)}")
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
            )) { asObjectFromJson(it, UploadResponse::class.java) }
        } catch (e: FileNotFoundException) {
            throw PackageException("Package file $file to be uploaded not found!", e)
        } catch (e: RequestException) {
            throw InstanceException("Cannot upload package $file to $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after uploading package $file to $instance.", e)
        }

        if (!response.isSuccess) {
            throw InstanceException("Cannot upload package $file to $instance. Reason: ${response.msg}.")
        }

        return response
    }

    fun downloadPackage(remotePath: String, targetFile: File, retry: Retry = Retry.none()) {
        lateinit var exception: FileException

        for (i in 0..retry.times) {
            try {
                downloadPackageOnce(remotePath, targetFile)
                return
            } catch (e: FileException) {
                exception = e

                if (i < retry.times) {
                    aem.logger.warn("Cannot download package $remotePath from $instance.")
                    aem.logger.debug("Download error", e)

                    val delay = retry.delay(i + 1)
                    val countdown = ProgressCountdown(project, delay)

                    aem.logger.lifecycle("Retrying download (${i + 1}/${retry.times}) after delay: ${Formats.duration(delay)}")
                    countdown.run()
                }
            }
        }

        throw exception
    }

    fun downloadPackageOnce(remotePath: String, targetFile: File) {
        aem.logger.info("Downloading package from $remotePath to file $targetFile")

        download(remotePath, targetFile)

        if (!targetFile.exists()) {
            throw InstanceException("Downloaded package is missing: ${targetFile.path}")
        }
    }

    fun buildPackage(remotePath: String): PackageBuildResponse {
        val url = "$PKG_MANAGER_JSON_PATH$remotePath/?cmd=build"

        aem.logger.info("Building package $remotePath on $instance")

        val response = try {
            postMultipart(url) { asObjectFromJson(it, PackageBuildResponse::class.java) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot build package $remotePath on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after building package $remotePath on $instance.", e)
        }

        if (!response.isSuccess) {
            throw InstanceException("Cannot build package $remotePath on $instance. Reason: ${response.msg}.")
        }
        return response
    }

    fun installPackage(remotePath: String, recursive: Boolean = true, retry: Retry = Retry.none()): InstallResponse {
        lateinit var exception: InstanceException
        for (i in 0..retry.times) {
            try {
                return installPackageOnce(remotePath, recursive)
            } catch (e: InstanceException) {
                exception = e
                if (i < retry.times) {
                    aem.logger.warn("Cannot install package $remotePath on $instance.")
                    aem.logger.debug("Install error", e)

                    val delay = retry.delay(i + 1)
                    val countdown = ProgressCountdown(project, delay)

                    aem.logger.lifecycle("Retrying install (${i + 1}/${retry.times}) after delay: ${Formats.duration(delay)}")
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
            throw InstanceException("Cannot install package $remotePath on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after installing package $remotePath on $instance.")
        }

        if (response.hasPackageErrors(aem.config.packageErrors)) {
            throw PackageException("Cannot install malformed package $remotePath on $instance. Status: ${response.status}. Errors: ${response.errors}")
        } else if (!response.success) {
            throw InstanceException("Cannot install package $remotePath on $instance. Status: ${response.status}. Errors: ${response.errors}")
        }

        return response
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, aem.config.packageSnapshots)
    }

    fun deployPackage(
        file: File,
        uploadForce: Boolean = true,
        uploadRetry: Retry = Retry.none(),
        installRecursive: Boolean = true,
        installRetry: Retry = Retry.none()
    ) {
        val uploadResponse = uploadPackage(file, uploadForce, uploadRetry)
        installPackage(uploadResponse.path, installRecursive, installRetry)
    }

    fun distributePackage(
        file: File,
        uploadForce: Boolean = true,
        uploadRetry: Retry = Retry.none(),
        installRecursive: Boolean = true,
        installRetry: Retry = Retry.none()
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
            postMultipart(url) { asObjectFromJson(it, UploadResponse::class.java) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot activate package $remotePath on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after activating package $remotePath on $instance.", e)
        }

        if (!response.isSuccess) {
            throw InstanceException("Cannot activate package $remotePath on $instance. Reason: ${response.msg}.")
        }

        return response
    }

    fun deletePackage(remotePath: String): DeleteResponse {
        val url = "$PKG_MANAGER_HTML_PATH$remotePath/?cmd=delete"

        aem.logger.info("Deleting package $remotePath on $instance")

        val response = try {
            postMultipart(url) { DeleteResponse.from(asStream(it), aem.config.packageResponseBuffer) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot delete package $remotePath from $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after deleting package $remotePath from $instance.", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot delete package $remotePath from $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun uninstallPackage(remotePath: String): UninstallResponse {
        val url = "$PKG_MANAGER_HTML_PATH$remotePath/?cmd=uninstall"

        aem.logger.info("Uninstalling package using command: $url")

        val response = try {
            postMultipart(url) { UninstallResponse.from(asStream(it), aem.config.packageResponseBuffer) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot uninstall package $remotePath on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after uninstalling package $remotePath from $instance.", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot uninstall package $remotePath from $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun determineInstanceState(): InstanceState {
        return InstanceState(this, instance)
    }

    fun determineBundleState(): BundleState {
        aem.logger.debug("Asking for OSGi bundles on $instance")

        return try {
            get(OSGI_BUNDLES_PATH) { asObjectFromJson(it, BundleState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot request OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }
    }

    fun determineComponentState(): ComponentState {
        aem.logger.debug("Asking for OSGi components on $instance")

        return try {
            get(OSGI_COMPONENTS_PATH) { asObjectFromJson(it, ComponentState::class.java) }
        } catch (e: AemException) {
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

    fun evalGroovyCode(code: String, data: Map<String, Any> = mapOf()): GroovyConsoleResult {
        return try {
            aem.logger.info("Executing Groovy Code: $code")
            evalGroovyCodeInternal(code, data)
        } catch (e: AemException) {
            throw InstanceException("Cannot evaluate Groovy code properly on $instance, code:\n$code", e)
        }
    }

    private fun evalGroovyCodeInternal(code: String, data: Map<String, Any>): GroovyConsoleResult {
        return postMultipart(GROOVY_CONSOLE_EVAL_PATH, mapOf(
                "script" to code,
                "data" to Formats.toJson(data)
        )) { asObjectFromJson(it, GroovyConsoleResult::class.java) }
    }

    fun evalGroovyScript(file: File, data: Map<String, Any> = mapOf()): GroovyConsoleResult {
        return try {
            aem.logger.info("Executing Groovy script: $file")
            evalGroovyCodeInternal(file.bufferedReader().use { it.readText() }, data)
        } catch (e: AemException) {
            throw InstanceException("Cannot evaluate Groovy script properly on $instance, file: $file", e)
        }
    }

    fun evalGroovyScript(fileName: String, data: Map<String, Any> = mapOf()): GroovyConsoleResult {
        val script = File(aem.config.groovyScriptRoot, fileName)
        if (!script.exists()) {
            throw AemException("Groovy script '$fileName' not found in directory: ${aem.config.groovyScriptRoot}")
        }

        return evalGroovyScript(script, data)
    }

    fun evalGroovyScripts(fileNamePattern: String = "**/*.groovy", data: Map<String, Any> = mapOf()): Sequence<GroovyConsoleResult> {
        val scripts = (project.file(aem.config.groovyScriptRoot).listFiles() ?: arrayOf()).filter {
            Patterns.wildcard(it, fileNamePattern)
        }.sortedBy { it.absolutePath }
        if (scripts.isEmpty()) {
            throw AemException("No Groovy scripts found in directory: ${aem.config.groovyScriptRoot}")
        }

        return evalGroovyScripts(scripts, data)
    }

    fun evalGroovyScripts(scripts: Collection<File>, data: Map<String, Any> = mapOf()): Sequence<GroovyConsoleResult> {
        return scripts.asSequence().map { evalGroovyScript(it, data) }
    }

    private fun shutdown(type: String) {
        try {
            aem.logger.info("Triggering shutdown of $instance.")
            postUrlencoded(OSGI_VMSTAT_PATH, mapOf("shutdown_type" to type))
        } catch (e: AemException) {
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

        const val GROOVY_CONSOLE_EVAL_PATH = "/bin/groovyconsole/post.json"
    }
}