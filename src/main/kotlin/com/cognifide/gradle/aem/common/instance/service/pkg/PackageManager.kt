package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.common.http.RequestException
import com.cognifide.gradle.common.http.ResponseException
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import java.io.File
import java.io.FileNotFoundException
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.Input

/**
 * Allows to communicate with CRX Package Manager.
 *
 * @see <https://helpx.adobe.com/experience-manager/6-5/sites/administering/using/package-manager.html>
 */
class PackageManager(sync: InstanceSync) : InstanceService(sync) {

    private val http = sync.http

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    val uploadForce = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.manager.uploadForce")?.let { set(it) }
    }

    /**
     * Repeat upload when failed (brute-forcing).
     */
    var uploadRetry = common.retry { afterSquaredSecond(aem.prop.long("package.manager.uploadRetry") ?: 3) }

    /**
     * Repeat install when failed (brute-forcing).
     */
    var installRetry = common.retry { afterSquaredSecond(aem.prop.long("package.manager.installRetry") ?: 2) }

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    val installRecursive = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.manager.installRecursive")?.let { set(it) }
    }

    /**
     * Allows to temporarily enable or disable workflows during CRX package deployment.
     */
    val workflowToggle = aem.obj.map<String, Boolean> {
        convention(mapOf())
        aem.prop.map("package.manager.workflowToggle")?.let { m -> set(m.mapValues { it.value.toBoolean() }) }
    }

    /**
     * Repeat listing package when failed (brute-forcing).
     */
    var listRetry = common.retry { afterSquaredSecond(aem.prop.long("package.manager.listRetry") ?: 3) }

    /**
     * Packages are installed lazy which means already installed will no be installed again.
     * By default, information about currently installed packages is being retrieved from AEM only once.
     *
     * This flag can change that behavior, so that information will be refreshed after each package installation.
     */
    @Input
    val listRefresh = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("package.manager.listRefresh")?.let { set(it) }
    }

    /**
     * Repeat download when failed (brute-forcing).
     */
    var downloadRetry = common.retry { afterSquaredSecond(aem.prop.long("package.manager.downloadRetry") ?: 3) }

    /**
     * Define patterns for known exceptions which could be thrown during package installation
     * making it impossible to succeed.
     *
     * When declared exception is encountered during package installation process, no more
     * retries will be applied.
     */
    val errors = aem.obj.strings {
        convention(listOf(
                "javax.jcr.nodetype.*Exception",
                "org.apache.jackrabbit.oak.api.*Exception",
                "org.apache.jackrabbit.vault.packaging.*Exception",
                "org.xml.sax.*Exception"
        ))
        aem.prop.list("package.manager.errors")?.let { set(it) }
    }

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified. Affects CRX packages composed and satisfied.
     */
    val snapshots = aem.obj.strings {
        convention(listOf())
        aem.prop.list("package.manager.snapshots")?.let { set(it) }
    }

    /**
     * Determines number of lines to process at once during reading Package Manager HTML responses.
     *
     * The higher the value, the bigger consumption of memory but shorter execution time.
     * It is a protection against exceeding max Java heap size.
     */
    val responseBuffer = aem.obj.int {
        convention(4096)
        aem.prop.int("package.manager.responseBuffer")?.let { set(it) }
    }

    fun get(file: File): Package {
        if (!file.exists()) {
            throw PackageException("Package $file does not exist so it cannot be resolved on $instance")
        }

        return find(file) ?: throw InstanceException("Package is not uploaded on $instance")
    }

    fun get(group: String, name: String, version: String): Package {
        return find(group, name, version)
                ?: throw InstanceException("Package ${Package.coordinates(group, name, version)}' is not uploaded on $instance")
    }

    fun find(file: File): Package? = PackageFile(file).run { find(group, name, version) }

    fun find(group: String, name: String, version: String): Package? = find { listResponse ->
        val expected = Package(group, name, version)

        logger.info("Finding package '${expected.coordinates}' on $instance")
        val actual = listResponse.resolvePackage(expected)
        if (actual == null) {
            logger.info("Package not found '${expected.coordinates}' on $instance")
        }

        actual
    }

    private fun find(resolver: (ListResponse) -> Package?): Package? {
        logger.debug("Asking for uploaded packages on $instance")
        return common.buildScope.getOrPut("instance.${instance.name}.packages", { list() }, listRefresh.get()).let(resolver)
    }

    fun list(): ListResponse {
        return listRetry.withCountdown<ListResponse, InstanceException>("list packages on '${instance.name}'") {
            return try {
                http.postMultipart(LIST_JSON) { asObjectFromJson(it, ListResponse::class.java) }
            } catch (e: RequestException) {
                throw InstanceException("Cannot list packages on $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after listing packages on $instance. Cause: ${e.message}", e)
            }
        }
    }

    fun upload(file: File): UploadResponse {
        return uploadRetry.withCountdown<UploadResponse, InstanceException>("upload package '${file.name}' on '${instance.name}'") {
            val url = "$JSON_PATH/?cmd=upload"

            logger.info("Uploading package $file to $instance'")

            val response = try {
                http.postMultipart(url, mapOf(
                        "package" to file,
                        "force" to (uploadForce.get() || isSnapshot(file))
                )) { asObjectFromJson(it, UploadResponse::class.java) }
            } catch (e: FileNotFoundException) {
                throw PackageException("Package file $file to be uploaded not found!", e)
            } catch (e: RequestException) {
                throw InstanceException("Cannot upload package $file to $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after uploading package $file to $instance. Cause: ${e.message}", e)
            }

            if (!response.isSuccess) {
                throw InstanceException("Cannot upload package $file to $instance. Reason: ${interpretFail(response.msg)}.")
            }

            return response
        }
    }

    /**
     * Create package on the fly, upload it to instance then build it.
     * Next built package is downloaded - replacing initially created package.
     * Finally built package is deleted on instance (preventing messing up).
     */
    fun download(definition: PackageDefinition.() -> Unit): File {
        val file = aem.composePackage {
            version.set(Formats.dateFileName())
            definition()
        }

        return downloadRetry.withCountdown<File, InstanceException>("download package '${file.name}' on '${instance.name}'") {
            var path: String? = null
            try {
                val pkg = upload(file)
                file.delete()

                path = pkg.path
                build(path)

                download(path, file)
            } finally {
                if (path != null) {
                    delete(path)
                }
            }

            return file
        }
    }

    fun download(remotePath: String, targetFile: File = common.temporaryFile(FilenameUtils.getName(remotePath))) {
        return downloadRetry.withCountdown<Unit, InstanceException>("download package '$remotePath' on '${instance.name}'") {
            logger.info("Downloading package from $remotePath to file $targetFile")

            http.fileTransfer { download(remotePath, targetFile) }

            if (!targetFile.exists()) {
                throw InstanceException("Downloaded package is missing: ${targetFile.path}")
            }
        }
    }

    fun build(remotePath: String): BuildResponse {
        val url = "$JSON_PATH$remotePath/?cmd=build"

        logger.info("Building package $remotePath on $instance")

        val response = try {
            http.postMultipart(url) { asObjectFromJson(it, BuildResponse::class.java) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot build package $remotePath on $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after building package $remotePath on $instance. Cause: ${e.message}", e)
        }

        if (!response.isSuccess) {
            throw InstanceException("Cannot build package $remotePath on $instance. Cause: ${interpretFail(response.msg)}")
        }

        return response
    }

    fun install(file: File) = install(get(file).path)

    fun install(remotePath: String): InstallResponse {
        return installRetry.withCountdown<InstallResponse, InstanceException>("install package '$remotePath' on '${instance.name}'") {
            val url = "$HTML_PATH$remotePath/?cmd=install"

            logger.info("Installing package $remotePath on $instance")

            val response = try {
                http.postMultipart(url, mapOf("recursive" to installRecursive.get())) {
                    InstallResponse.from(asStream(it), responseBuffer.get())
                }
            } catch (e: RequestException) {
                throw InstanceException("Cannot install package $remotePath on $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after installing package $remotePath on $instance. Cause: ${e.message}", e)
            }

            if (response.hasPackageErrors(errors.get())) {
                throw PackageException("Cannot install malformed package $remotePath on $instance. Status: ${response.status}. Errors: ${response.errors}")
            } else if (!response.success) {
                throw InstanceException("Cannot install package $remotePath on $instance. Status: ${response.status}. Errors: ${response.errors}")
            }

            return response
        }
    }

    private fun interpretFail(message: String): String = when (message) {
        "Inaccessible value" -> "Probably no disk space left (server respond with '$message')" // https://forums.adobe.com/thread/2338290
        else -> message
    }

    fun isSnapshot(file: File): Boolean = Patterns.wildcard(file, snapshots.get())

    fun deploy(file: File, activate: Boolean = false) {
        val uploadResponse = upload(file)
        val packagePath = uploadResponse.path

        sync.workflowManager.toggleTemporarily(workflowToggle.get()) {
            install(packagePath)
            if (activate) {
                activate(packagePath)
            }
        }
    }

    fun distribute(file: File) = deploy(file, true)

    fun activate(file: File) = activate(get(file).path)

    fun activate(remotePath: String): UploadResponse {
        val url = "$JSON_PATH$remotePath/?cmd=replicate"

        logger.info("Activating package $remotePath on $instance")

        val response = try {
            http.postMultipart(url) { asObjectFromJson(it, UploadResponse::class.java) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot activate package $remotePath on $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after activating package $remotePath on $instance. Cause: ${e.message}", e)
        }

        if (!response.isSuccess) {
            throw InstanceException("Cannot activate package $remotePath on $instance. Cause: ${interpretFail(response.msg)}")
        }

        return response
    }

    fun delete(file: File) = delete(get(file).path)

    fun delete(remotePath: String): DeleteResponse {
        val url = "$HTML_PATH$remotePath/?cmd=delete"

        logger.info("Deleting package $remotePath on $instance")

        val response = try {
            http.postMultipart(url) { DeleteResponse.from(asStream(it), responseBuffer.get()) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot delete package $remotePath from $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after deleting package $remotePath from $instance. Cause: ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot delete package $remotePath from $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun uninstall(file: File) = uninstall(get(file).path)

    fun uninstall(remotePath: String): UninstallResponse {
        val url = "$HTML_PATH$remotePath/?cmd=uninstall"

        logger.info("Uninstalling package using command: $url")

        val response = try {
            http.postMultipart(url) { UninstallResponse.from(asStream(it), responseBuffer.get()) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot uninstall package $remotePath on $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after uninstalling package $remotePath from $instance. Cause ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot uninstall package $remotePath from $instance. Status: ${response.status}. Errors: ${response.errors}.")
        }

        return response
    }

    fun purge(file: File) {
        try {
            val pkg = get(file)

            try {
                uninstall(pkg.path)
            } catch (e: InstanceException) {
                logger.info("${e.message} Is it installed already?")
                logger.debug("Cannot uninstall package.", e)
            }

            try {
                delete(pkg.path)
            } catch (e: InstanceException) {
                logger.info(e.message)
                logger.debug("Cannot delete package.", e)
            }
        } catch (e: InstanceException) {
            aem.logger.info(e.message)
            aem.logger.debug("Nothing to purge.", e)
        }
    }

    companion object {
        const val PATH = "/crx/packmgr/service"

        const val JSON_PATH = "$PATH/.json"

        const val HTML_PATH = "$PATH/.html"

        const val LIST_JSON = "/crx/packmgr/list.jsp"
    }
}
