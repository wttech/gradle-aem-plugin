package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.aem.common.pkg.vault.VaultDefinition
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.http.HttpClient
import com.cognifide.gradle.common.http.RequestException
import com.cognifide.gradle.common.http.ResponseException
import com.cognifide.gradle.common.pluginProject
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.input.TeeInputStream
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.gradle.api.tasks.Input
import org.gradle.process.internal.streams.SafeStreams
import java.io.File
import java.io.FileNotFoundException

/**
 * Allows to communicate with CRX Package Manager.
 *
 * @see <https://helpx.adobe.com/experience-manager/6-5/sites/administering/using/package-manager.html>
 */
@Suppress("TooManyFunctions")
class PackageManager(sync: InstanceSync) : InstanceService(sync) {

    private val http = sync.http

    /**
     * Check if console is installed on instance.
     */
    val available: Boolean get() = try {
        http.head(INDEX_PATH) { it.statusLine.statusCode == HttpStatus.SC_OK }
    } catch (e: CommonException) {
        logger.debug("Seems that package manager is not available: $instance", e)
        false
    }

    /**
     * Ensure by throwing exception that package manager is available on instance.
     */
    fun requireAvailable() {
        if (!available) {
            throw InstanceException(
                "Package manager is not available on $instance!\n" +
                    "Ensure having correct URLs defined & credentials, granted access and networking in correct state (internet accessible, VPN on/off)"
            )
        }
    }

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    val uploadForce = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.packageManager.uploadForce")?.let { set(it) }
    }

    /**
     * Repeat upload when failed (brute-forcing).
     */
    var uploadRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.packageManager.uploadRetry") ?: 3) }

    /**
     * Repeat install when failed (brute-forcing).
     */
    var installRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.packageManager.installRetry") ?: 3) }

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    val installRecursive = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.packageManager.installRecursive")?.let { set(it) }
    }

    /**
     * Deploys only if package is changed (checksum based) or reinstalled on instance in the meantime.
     */
    val deployAvoidance = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.packageManager.deployAvoidance")?.let { set(it) }
    }

    /**
     * Allows to temporarily enable or disable workflows during CRX package deployment.
     */
    val workflowToggle = aem.obj.map<String, Boolean> {
        convention(mapOf())
        aem.prop.map("instance.packageManager.workflowToggle")?.let { m -> set(m.mapValues { it.value.toBoolean() }) }
    }

    /**
     * Repeat listing package when failed (brute-forcing).
     */
    var listRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.packageManager.listRetry") ?: 3) }

    /**
     * Packages are installed lazy which means already installed will no be installed again.
     * By default, information about currently installed packages is being retrieved from AEM only once.
     *
     * This flag can change that behavior, so that information will be refreshed after each package installation.
     */
    @Input
    val listRefresh = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.packageManager.listRefresh")?.let { set(it) }
    }

    /**
     * Repeat download when failed (brute-forcing).
     */
    var downloadRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.packageManager.downloadRetry") ?: 2) }

    /**
     * Delete package after download (also when failed).
     */
    val downloadClean = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.packageManager.downloadClean")?.let { set(it) }
    }

    /**
     * Define patterns for known exceptions which could be thrown during package installation
     * making it impossible to succeed.
     *
     * When declared exception is encountered during package installation process, no more
     * retries will be applied.
     */
    val errors = aem.obj.strings {
        convention(
            listOf(
                "javax.jcr.nodetype.*Exception",
                "org.apache.jackrabbit.oak.api.*Exception",
                "org.apache.jackrabbit.vault.packaging.*Exception",
                "org.xml.sax.*Exception"
            )
        )
        aem.prop.list("instance.packageManager.errors")?.let { set(it) }
    }

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified. Affects CRX packages composed and satisfied.
     */
    val snapshots = aem.obj.strings {
        convention(listOf())
        aem.prop.list("instance.packageManager.snapshots")?.let { set(it) }
    }

    /**
     * Determines number of lines to process at once during reading Package Manager HTML responses.
     *
     * The higher the value, the bigger consumption of memory but shorter execution time.
     * It is a protection against exceeding max Java heap size.
     */
    val responseBuffer = aem.obj.int {
        convention(4096)
        aem.prop.int("instance.packageManager.responseBuffer")?.let { set(it) }
    }

    /**
     * Controls dumping of package installation responses.
     *
     * Enable only for debugging purposes. Increases deployment time about n-times.
     */
    val responseHandling = aem.obj.typed<ResponseHandling> {
        convention(ResponseHandling.NONE)
        aem.prop.string("instance.packageManager.responseHandling")?.let { set(ResponseHandling.of(it)) }
    }

    /**
     * Customize response handling.
     */
    fun responseHandling(name: String) {
        responseHandling.set(ResponseHandling.of(name))
    }

    /**
     * Location of dumped package installation responses.
     */
    val responseDir = aem.obj.dir {
        convention((project.pluginProject(InstancePlugin.ID) ?: project.rootProject).layout.buildDirectory.dir("package/install"))
        aem.prop.file("instance.packageManager.responseDir")?.let { set(it) }
    }

    /**
     * Repeat reading package metadata when failed (brute-forcing).
     */
    var metadataRetry = common.retry { afterSquaredSecond(aem.prop.long("instance.packageManager.metadataRetry") ?: 3) }

    fun get(file: File): Package {
        if (!file.exists()) {
            throw PackageException("Package '$file' does not exist so it cannot be resolved on $instance!")
        }

        return find(file) ?: throw InstanceException("Package '$file' is not uploaded on $instance!")
    }

    fun get(definition: VaultDefinition) = get(definition.group.get(), definition.name.get(), definition.version.get())

    fun get(group: String, name: String, version: String): Package {
        return find(group, name, version)
            ?: throw InstanceException("Package ${Package.coordinates(group, name, version)}' is not uploaded on $instance!")
    }

    fun find(file: File): Package? = PackageFile(file).run { find(group, name, version) }

    fun find(definition: VaultDefinition) = find(definition.group.get(), definition.name.get(), definition.version.get())

    fun find(group: String, name: String, version: String): Package? = find { it.resolve(group, name, version) }

    fun findAll(group: String, name: String) = find { it.results.filter { pkg -> pkg.group == group && pkg.name == name } }

    private fun <T> find(resolver: (ListResponse) -> T): T {
        logger.debug("Asking for uploaded packages on $instance")
        return common.buildScope.getOrPut("instance.${instance.name}.packages", { list() }, listRefresh.get()).let(resolver)
    }

    fun contains(pkg: Package, checkSize: Boolean = true): Boolean = all
        .firstOrNull { it == pkg }
        ?.let { !checkSize || it.size == pkg.size }
        ?: false

    fun contains(file: File, checkSize: Boolean): Boolean = find(file)
        ?.let { !checkSize || it.size == file.length() }
        ?: false

    operator fun contains(file: File): Boolean = contains(file, true)

    fun list(): ListResponse {
        return listRetry.withCountdown<ListResponse, InstanceException>("list packages on '${instance.name}'") {
            try {
                http.postMultipart(LIST_JSON) { asObjectFromJson(it, ListResponse::class.java) }
            } catch (e: RequestException) {
                throw InstanceException("Cannot list packages on $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after listing packages on $instance. Cause: ${e.message}", e)
            }.apply {
                this.instance = this@PackageManager.instance
                this.results.forEach { it.instance = this@PackageManager.instance }
            }
        }
    }

    val all get() = list().results

    val installed get() = all.filter { it.installed }

    fun upload(file: File): UploadResponse {
        return uploadRetry.withCountdown<UploadResponse, InstanceException>("upload package '${file.name}' on '${instance.name}'") {
            val url = "$JSON_PATH/?cmd=upload"

            logger.info("Uploading package '$file' to $instance'")

            val response = try {
                http.postMultipart(
                    url,
                    mapOf(
                        "package" to file,
                        "force" to (uploadForce.get() || isSnapshot(file))
                    )
                ) { asObjectFromJson(it, UploadResponse::class.java) }
            } catch (e: FileNotFoundException) {
                throw PackageException("Package '$file' to be uploaded not found!", e)
            } catch (e: RequestException) {
                throw InstanceException("Cannot upload package '$file' to $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after uploading package '$file' to $instance. Cause: ${e.message}", e)
            }

            if (!response.success) {
                throw InstanceException("Cannot upload package '$file' to $instance. Reason: ${interpretFail(response.msg)}.")
            }

            return response
        }
    }

    /**
     * Create package on the fly, upload it to instance then build it.
     * Next built package is downloaded - replacing initially created package.
     * Finally built package is deleted on instance (preventing messing up).
     */
    fun download(definition: PackageDefinition.() -> Unit) = download(
        PackageDefinition(aem).apply {
            version.set(Formats.dateFileName())
            definition()
        }
    )

    /**
     * Create package on the fly, upload it to instance then build it.
     * Next built package is downloaded - replacing initially created package.
     * Finally built package is deleted on instance (preventing messing up).
     */
    fun download(definition: PackageDefinition): File {
        val file = definition.compose()

        return downloadRetry.withCountdown<File, InstanceException>("download package '${file.name}' on '${instance.name}'") {
            var path: String? = null
            try {
                val pkg = upload(file)
                file.delete()

                path = pkg.path
                build(path)

                download(path, file)
            } finally {
                if (downloadClean.get() && path != null) {
                    delete(path)
                }
            }

            return file
        }
    }

    fun download(pkg: Package, targetFile: File = common.temporaryFile(pkg.downloadName)) = download(pkg.path, targetFile)

    fun download(remotePath: String, targetFile: File = common.temporaryFile(FilenameUtils.getName(remotePath))) {
        return downloadRetry.withCountdown<Unit, InstanceException>("download package '$remotePath' on '${instance.name}'") {
            logger.info("Downloading package from '$remotePath' to file '$targetFile'")

            http.download(remotePath, targetFile)

            if (!targetFile.exists()) {
                throw InstanceException("Downloaded package is missing: ${targetFile.path}!")
            }
        }
    }

    fun downloadTo(pkg: Package, targetDir: File): File = downloadTo(pkg.path, targetDir)

    fun downloadTo(remotePath: String, targetDir: File): File = targetDir.resolve(FilenameUtils.getName(remotePath)).also { download(remotePath, it) }

    fun build(file: File) = build(get(file))

    fun build(pkg: Package) = build(pkg.path)

    fun build(remotePath: String): BuildResponse {
        val url = "$JSON_PATH$remotePath/?cmd=build"

        logger.info("Building package '$remotePath' on $instance")

        val response = try {
            http.postMultipart(url) { asObjectFromJson(it, BuildResponse::class.java) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot build package '$remotePath' on $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after building package '$remotePath' on $instance. Cause: ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot build package '$remotePath' on $instance. Cause: ${interpretFail(response.msg)}")
        }

        return response
    }

    fun install(file: File) = install(get(file).path)

    fun install(pkg: Package) = install(pkg.path)

    fun install(remotePath: String): InstallResponse {
        return installRetry.withCountdown<InstallResponse, InstanceException>("install package '$remotePath' on '${instance.name}'") {
            val url = "$HTML_PATH$remotePath/?cmd=install"

            logger.info("Installing package '$remotePath' on $instance")

            val response = try {
                http.postMultipart(url, mapOf("recursive" to installRecursive.get())) { handleInstallResponse(remotePath, it) }
            } catch (e: RequestException) {
                throw InstanceException("Cannot install package '$remotePath' on $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after installing package '$remotePath' on $instance. Cause: ${e.message}", e)
            }

            if (response.hasPackageErrors(errors.get())) {
                throw PackageException("Cannot install malformed package '$remotePath' on $instance. Status: ${response.status}. Errors: ${response.errors}")
            } else if (!response.success) {
                throw InstanceException("Cannot install package '$remotePath' on $instance. Status: ${response.status}. Errors: ${response.errors}")
            }

            return response
        }
    }

    private fun HttpClient.handleInstallResponse(remotePath: String, response: HttpResponse): InstallResponse = when (responseHandling.get()) {
        ResponseHandling.FILE -> {
            val dumpFile = responseDir.map { it.dir("$it/${instance.name}") }
                .get().asFile
                .resolve(remotePath.removePrefix("/"))
                .apply {
                    delete()
                    parentFile.mkdirs()
                }
                .run { parentFile.resolve("$nameWithoutExtension.html") }
            logger.info("Dumping package installation response to file '$dumpFile'")
            val teeOut = dumpFile.outputStream().buffered()
            val teeIn = TeeInputStream(asStream(response).buffered(), teeOut, true)
            InstallResponse.from(teeIn, responseBuffer.get())
        }
        ResponseHandling.CONSOLE -> {
            logger.info("Printing package installation response to console")
            val teeOut = SafeStreams.systemOut().buffered()
            val teeIn = TeeInputStream(asStream(response).buffered(), teeOut, true)
            InstallResponse.from(teeIn, responseBuffer.get())
        }
        else -> InstallResponse.from(asStream(response), responseBuffer.get())
    }

    private fun interpretFail(message: String): String = when (message) {
        "Inaccessible value" -> "Probably no disk space left (server respond with '$message')" // https://forums.adobe.com/thread/2338290
        else -> message
    }

    fun isSnapshot(file: File): Boolean = Patterns.wildcard(file, snapshots.get())

    fun isDeployed(file: File): Boolean {
        val pkg = find(file) ?: return false
        return isDeployed(pkg)
    }

    fun isDeployed(pkg: Package): Boolean {
        if (!pkg.installed) return false
        val otherVersions = findAll(pkg.group, pkg.name).filter { it != pkg }
        return otherVersions.none { (it.lastUnpacked ?: 0) > (pkg.lastUnpacked ?: 0) }
    }

    fun deploy(file: File, activate: Boolean = false): Boolean {
        if (deployAvoidance.get()) {
            return deployAvoiding(file, activate)
        }

        deployRegularly(file, activate)
        return true
    }

    private fun deployAvoiding(file: File, activate: Boolean): Boolean {
        val pkg = find(file)
        val checksum by lazy {
            Formats.toChecksumFile(file).let { checksumFile ->
                if (checksumFile.exists()) {
                    checksumFile.readText()
                } else {
                    logger.info("Calculating checksum of package to be deployed '$file'")
                    Formats.toChecksum(file)
                }
            }
        }

        if (pkg == null || !isDeployed(pkg)) {
            deployRegularly(file, activate, checksum)
            return true
        } else {
            val path = pkg.path
            val deployment = metadataRetry.withCountdown<PackageDeployment, InstanceException>(
                "examine deployment of package '${pkg.path}' on '${instance.name}'"
            ) {
                PackageMetadata(this, path).examineDeployment(checksum)
            }
            return if (deployment.needed) {
                if (deployment.checksumChanged) {
                    logger.info("Deploying package '$path' on $instance (changed checksum)")
                }
                if (deployment.installedExternally) {
                    logger.warn("Deploying package '$path' on $instance (installed externally at '${deployment.installedExternallyAt}')")
                }
                deployRegularly(file, activate, checksum)
                true
            } else {
                logger.lifecycle("No need to deploy package '$path' on $instance (no changes)")
                false
            }
        }
    }

    private fun deployRegularly(file: File, activate: Boolean = false, checksum: String? = null): String {
        val uploadResponse = upload(file)
        val packagePath = uploadResponse.path

        sync.workflowManager.toggleTemporarily(workflowToggle.get()) {
            when {
                checksum != null -> PackageMetadata(this, packagePath).update(checksum) { install(packagePath) }
                else -> install(packagePath)
            }
            if (activate) {
                activate(packagePath)
            }
        }

        return packagePath
    }

    fun distribute(file: File) = deploy(file, true)

    fun activate(file: File) = activate(get(file).path)

    fun activate(pkg: Package) = activate(pkg.path)

    fun activate(remotePath: String): UploadResponse {
        val url = "$JSON_PATH$remotePath/?cmd=replicate"

        logger.info("Activating package '$remotePath' on $instance")

        val response = try {
            http.postMultipart(url) { asObjectFromJson(it, UploadResponse::class.java) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot activate package '$remotePath' on $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after activating package '$remotePath' on $instance. Cause: ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot activate package '$remotePath' on $instance. Cause: ${interpretFail(response.msg)}")
        }

        return response
    }

    fun delete(file: File) = delete(get(file).path)

    fun delete(pkg: Package) = delete(pkg.path)

    fun delete(remotePath: String): DeleteResponse {
        val url = "$HTML_PATH$remotePath/?cmd=delete"

        logger.info("Deleting package '$remotePath' on $instance")

        val response = try {
            http.postMultipart(url) { DeleteResponse.from(asStream(it), responseBuffer.get()) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot delete package '$remotePath' from $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after deleting package '$remotePath' from $instance. Cause: ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot delete package '$remotePath' from $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun uninstall(file: File) = uninstall(get(file).path)

    fun uninstall(pkg: Package) = uninstall(pkg.path)

    fun uninstall(remotePath: String): UninstallResponse {
        val url = "$HTML_PATH$remotePath/?cmd=uninstall"

        logger.info("Uninstalling package '$remotePath' on $instance")

        val response = try {
            http.postMultipart(url) { UninstallResponse.from(asStream(it), responseBuffer.get()) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot uninstall package '$remotePath' on $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after uninstalling package '$remotePath' from $instance. Cause ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot uninstall package '$remotePath' from $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun purge(file: File) = purge(get(file))

    fun purge(pkg: Package) = purge(pkg.path)

    fun purge(remotePath: String): Boolean {
        var purged = false
        try {
            try {
                uninstall(remotePath)
                purged = true
            } catch (e: InstanceException) {
                logger.info("${e.message} Is it installed already?")
                logger.debug("Cannot uninstall package.", e)
            }

            try {
                delete(remotePath)
                purged = true
            } catch (e: InstanceException) {
                logger.info(e.message)
                logger.debug("Cannot delete package.", e)
            }
        } catch (e: InstanceException) {
            aem.logger.info(e.message)
            aem.logger.debug("Nothing to purge.", e)
        }
        return purged
    }

    enum class ResponseHandling {
        FILE,
        CONSOLE,
        NONE;

        companion object {
            fun of(name: String) = values().find { it.name.equals(name, true) }
                ?: throw AemException("Unsupported package response handling: $name")
        }
    }

    companion object {
        const val PATH = "/crx/packmgr/service"

        const val JSON_PATH = "$PATH/.json"

        const val HTML_PATH = "$PATH/.html"

        const val LIST_JSON = "/crx/packmgr/list.jsp"

        const val INDEX_PATH = "/crx/packmgr/index.jsp"
    }
}
