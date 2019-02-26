package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.*
import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.pkg.*
import com.cognifide.gradle.aem.pkg.tasks.Compose
import java.io.File
import java.io.FileNotFoundException
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.zeroturnaround.zip.ZipUtil

@Suppress("LargeClass", "TooManyFunctions")
class InstanceSync(aem: AemExtension, instance: Instance) : InstanceHttpClient(aem, instance) {

    fun getPackage(file: File, refresh: Boolean = true, retry: Retry = aem.retry()): Package {
        if (!file.exists()) {
            throw PackageException("Package $file does not exist so it cannot be resolved on $instance")
        }

        return resolvePackage(file, refresh, retry)
                ?: throw InstanceException("Package is not uploaded on $instance")
    }

    fun getPackage(group: String, name: String, version: String, refresh: Boolean = true, retry: Retry = aem.retry()): Package {
        return resolvePackage(group, name, version, refresh, retry)
                ?: throw InstanceException("Package ${Package.coordinates(group, name, version)}' is not uploaded on $instance")
    }

    fun resolvePackage(file: File, refresh: Boolean = true, retry: Retry = aem.retry()): Package? {
        if (!ZipUtil.containsEntry(file, Package.VLT_PROPERTIES)) {
            throw PackageException("File is not a valid CRX package: $file")
        }

        val xml = ZipUtil.unpackEntry(file, Package.VLT_PROPERTIES).toString(Charsets.UTF_8)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())

        val group = doc.select("entry[key=group]").text()
        val name = doc.select("entry[key=name]").text()
        val version = doc.select("entry[key=version]").text()

        return resolvePackage(group, name, version, refresh, retry)
    }

    fun resolvePackage(compose: Compose, refresh: Boolean = true, retry: Retry = aem.retry()): Package? {
        return resolvePackage({ it.resolvePackage(project, Package(compose)) }, refresh, retry)
    }

    fun resolvePackage(group: String, name: String, version: String, refresh: Boolean = true, retry: Retry = aem.retry()): Package? {
        return resolvePackage({ it.resolvePackage(project, Package(group, name, version)) }, refresh, retry)
    }

    private fun resolvePackage(resolver: (ListResponse) -> Package?, refresh: Boolean, retry: Retry = aem.retry()): Package? {
        aem.logger.debug("Asking for uploaded packages on $instance")

        val packages = BuildScope.of(project).getOrPut("instance.${instance.name}.packages", {
            listPackages(retry)
        }, refresh)

        return resolver(packages)
    }

    fun listPackages(retry: Retry = aem.retry()): ListResponse {
        return retry.launch<ListResponse, InstanceException>("list packages") {
            listPackagesOnce()
        }
    }

    private fun listPackagesOnce(): ListResponse {
        return try {
            postMultipart(PKG_MANAGER_LIST_JSON) { asObjectFromJson(it, ListResponse::class.java) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot list packages on $instance. Reason: request failed.", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after listing packages on $instance.", e)
        }
    }

    fun uploadPackage(file: File, force: Boolean = true, retry: Retry = aem.retry()): UploadResponse {
        return retry.launch<UploadResponse, InstanceException>("upload package") {
            uploadPackageOnce(file, force)
        }
    }

    private fun uploadPackageOnce(file: File, force: Boolean): UploadResponse {
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
            throw InstanceException("Cannot upload package $file to $instance. Reason: ${interpretFail(response.msg)}.")
        }

        return response
    }

    fun downloadPackage(remotePath: String, targetFile: File, retry: Retry = aem.retry()) {
        return retry.launch<Unit, InstanceException>("download package") {
            downloadPackageOnce(remotePath, targetFile)
        }
    }

    private fun downloadPackageOnce(remotePath: String, targetFile: File) {
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
            throw InstanceException("Cannot build package $remotePath on $instance. Reason: ${interpretFail(response.msg)}.")
        }
        return response
    }

    fun installPackage(remotePath: String, recursive: Boolean = true, retry: Retry = aem.retry()): InstallResponse {
        return retry.launch<InstallResponse, InstanceException>("install package") {
            installPackageOnce(remotePath, recursive)
        }
    }

    private fun installPackageOnce(remotePath: String, recursive: Boolean = true): InstallResponse {
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

    private fun interpretFail(message: String): String = when (message) {
        // https://forums.adobe.com/thread/2338290
        "Inaccessible value" -> "no disk space left (server respond with '$message'})"
        else -> message
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, aem.config.packageSnapshots)
    }

    fun deployPackage(
        file: File,
        uploadForce: Boolean = true,
        uploadRetry: Retry = aem.retry(),
        installRecursive: Boolean = true,
        installRetry: Retry = aem.retry()
    ) {
        val uploadResponse = uploadPackage(file, uploadForce, uploadRetry)
        installPackage(uploadResponse.path, installRecursive, installRetry)
    }

    fun distributePackage(
        file: File,
        uploadForce: Boolean = true,
        uploadRetry: Retry = aem.retry(),
        installRecursive: Boolean = true,
        installRetry: Retry = aem.retry()
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
            throw InstanceException("Cannot activate package $remotePath on $instance. Reason: ${interpretFail(response.msg)}.")
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
            get(OSGI_BUNDLES_LIST_JSON) { asObjectFromJson(it, BundleState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot request OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }
    }

    fun findBundle(symbolicName: String): Bundle? {
        return determineBundleState().bundles.find {
            symbolicName.equals(it.symbolicName, ignoreCase = true)
        }
    }

    fun getBundle(symbolicName: String): Bundle {
        return findBundle(symbolicName)
                ?: throw InstanceException("OSGi bundle '$symbolicName' cannot be found on $instance.")
    }

    fun startBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        if (bundle.stable) {
            aem.logger.info("Not starting already started OSGi $bundle on $instance.")
            return
        }

        aem.logger.info("Starting OSGi $bundle on $instance.")
        post("$OSGI_BUNDLES_PATH/${bundle.id}", mapOf("action" to "start"))
    }

    fun stopBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        if (!bundle.stable) {
            aem.logger.info("Not stopping already stopped OSGi $bundle on $instance.")
            return
        }

        aem.logger.info("Stopping OSGi $bundle on $instance.")
        post("$OSGI_BUNDLES_PATH/${bundle.id}", mapOf("action" to "stop"))
    }

    fun restartBundle(symbolicName: String) {
        stopBundle(symbolicName)
        startBundle(symbolicName)
    }

    fun refreshBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        aem.logger.info("Refreshing OSGi $bundle on $instance.")
        post("$OSGI_BUNDLES_PATH/${bundle.symbolicName}", mapOf("action" to "refresh"))
    }

    fun updateBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        aem.logger.info("Updating OSGi $bundle on $instance.")
        post("$OSGI_BUNDLES_PATH/${bundle.symbolicName}", mapOf("action" to "update"))
    }

    fun determineComponentState(): ComponentState {
        aem.logger.debug("Asking for OSGi components on $instance")

        return try {
            get(OSGI_COMPONENTS_LIST_JSON) { asObjectFromJson(it, ComponentState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot determine OSGi components state on $instance", e)
            ComponentState.unknown()
        }
    }

    fun findComponent(pid: String): Component? {
        return determineComponentState().components.find {
            pid.equals(it.pid, ignoreCase = true)
        }
    }

    fun getComponent(pid: String): Component {
        return findComponent(pid) ?: throw InstanceException("OSGi component '$pid' cannot be found on $instance.")
    }

    fun enableComponent(pid: String) {
        val component = getComponent(pid)
        if (component.id.isNotBlank()) {
            aem.logger.info("Not enabling already enabled OSGi $component on $instance.")
            return
        }

        aem.logger.info("Enabling OSGi $component on $instance.")
        post("$OSGI_COMPONENTS_PATH/${component.pid}", mapOf("action" to "enable"))
    }

    fun disableComponent(pid: String) {
        val component = getComponent(pid)
        if (component.id.isBlank()) {
            aem.logger.info("Not disabling already disabled OSGi $component on $instance.")
            return
        }

        aem.logger.info("Disabling OSGi $component on $instance.")
        post("$OSGI_COMPONENTS_PATH/${component.id}", mapOf("action" to "disable"))
    }

    fun restartComponent(pid: String) {
        disableComponent(pid)
        enableComponent(pid)
    }

    fun restartFramework() = shutdownFramework("Restart")

    fun stopFramework() = shutdownFramework("Stop")

    private fun shutdownFramework(type: String) {
        try {
            aem.logger.info("Triggering OSGi framework shutdown on $instance.")
            postUrlencoded(OSGI_VMSTAT_PATH, mapOf("shutdown_type" to type))
        } catch (e: AemException) {
            throw InstanceException("Cannot trigger shutdown of $instance.", e)
        }
    }

    fun evalGroovyCode(code: String, data: Map<String, Any> = mapOf(), verbose: Boolean = true): GroovyConsoleResult {
        val result = try {
            aem.logger.info("Executing Groovy Code: $code")
            evalGroovyCodeInternal(code, data)
        } catch (e: AemException) {
            throw InstanceException("Cannot evaluate Groovy code properly on $instance, code:\n$code", e)
        }

        if (verbose && result.exceptionStackTrace.isNotBlank()) {
            aem.logger.debug(result.toString())
            throw InstanceException("Execution of Groovy code on $instance ended with exception:\n${result.exceptionStackTrace}")
        }

        return result
    }

    private fun evalGroovyCodeInternal(code: String, data: Map<String, Any>): GroovyConsoleResult {
        return postMultipart(GROOVY_CONSOLE_EVAL_PATH, mapOf(
                "script" to code,
                "data" to Formats.toJson(data)
        )) { asObjectFromJson(it, GroovyConsoleResult::class.java) }
    }

    fun evalGroovyScript(file: File, data: Map<String, Any> = mapOf(), verbose: Boolean = true): GroovyConsoleResult {
        val result = try {
            aem.logger.info("Executing Groovy script: $file")
            evalGroovyCodeInternal(file.bufferedReader().use { it.readText() }, data)
        } catch (e: AemException) {
            throw InstanceException("Cannot evaluate Groovy script properly on $instance, file: $file", e)
        }

        if (verbose && result.exceptionStackTrace.isNotBlank()) {
            aem.logger.debug(result.toString())
            throw InstanceException("Execution of Groovy script $file on $instance ended with exception:\n${result.exceptionStackTrace}")
        }

        return result
    }

    fun evalGroovyScript(fileName: String, data: Map<String, Any> = mapOf(), verbose: Boolean = true): GroovyConsoleResult {
        val script = File(aem.config.groovyScriptRoot, fileName)
        if (!script.exists()) {
            throw AemException("Groovy script '$fileName' not found in directory: ${aem.config.groovyScriptRoot}")
        }

        return evalGroovyScript(script, data, verbose)
    }

    fun evalGroovyScripts(fileNamePattern: String = "**/*.groovy", data: Map<String, Any> = mapOf(), verbose: Boolean = true): Sequence<GroovyConsoleResult> {
        val scripts = (project.file(aem.config.groovyScriptRoot).listFiles() ?: arrayOf()).filter {
            Patterns.wildcard(it, fileNamePattern)
        }.sortedBy { it.absolutePath }
        if (scripts.isEmpty()) {
            throw AemException("No Groovy scripts found in directory: ${aem.config.groovyScriptRoot}")
        }

        return evalGroovyScripts(scripts, data, verbose)
    }

    fun evalGroovyScripts(scripts: Collection<File>, data: Map<String, Any> = mapOf(), verbose: Boolean = true): Sequence<GroovyConsoleResult> {
        return scripts.asSequence().map { evalGroovyScript(it, data, verbose) }
    }

    companion object {
        const val PKG_MANAGER_PATH = "/crx/packmgr/service"

        const val PKG_MANAGER_JSON_PATH = "$PKG_MANAGER_PATH/.json"

        const val PKG_MANAGER_HTML_PATH = "$PKG_MANAGER_PATH/.html"

        const val PKG_MANAGER_LIST_JSON = "/crx/packmgr/list.jsp"

        const val OSGI_BUNDLES_PATH = "/system/console/bundles"

        const val OSGI_BUNDLES_LIST_JSON = "$OSGI_BUNDLES_PATH.json"

        const val OSGI_COMPONENTS_PATH = "/system/console/components"

        const val OSGI_COMPONENTS_LIST_JSON = "$OSGI_COMPONENTS_PATH.json"

        const val OSGI_VMSTAT_PATH = "/system/console/vmstat"

        const val GROOVY_CONSOLE_EVAL_PATH = "/bin/groovyconsole/post.json"
    }
}