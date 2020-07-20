package com.cognifide.gradle.sling.common.instance.service.pkg

import com.cognifide.gradle.sling.common.instance.InstanceException
import com.cognifide.gradle.sling.common.instance.InstanceService
import com.cognifide.gradle.sling.common.instance.InstanceSync
import com.cognifide.gradle.sling.common.instance.service.repository.Node
import com.cognifide.gradle.sling.common.pkg.PackageDefinition
import com.cognifide.gradle.sling.common.pkg.PackageException
import com.cognifide.gradle.sling.common.pkg.PackageFile
import com.cognifide.gradle.sling.common.pkg.vault.VaultDefinition
import com.cognifide.gradle.sling.common.utils.Checksum
import com.cognifide.gradle.common.http.RequestException
import com.cognifide.gradle.common.http.ResponseException
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import java.io.File
import java.io.FileNotFoundException
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.Input
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * Allows to communicate with CRX Package Manager.
 *
 * @see <https://helpx.adobe.com/experience-manager/6-5/sites/administering/using/package-manager.html>
 */
@Suppress("TooManyFunctions")
class PackageManager(sync: InstanceSync) : InstanceService(sync) {

    private val http = sync.http

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    val uploadForce = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.manager.uploadForce")?.let { set(it) }
    }

    /**
     * Repeat upload when failed (brute-forcing).
     */
    var uploadRetry = common.retry { afterSquaredSecond(sling.prop.long("package.manager.uploadRetry") ?: 3) }

    /**
     * Repeat install when failed (brute-forcing).
     */
    var installRetry = common.retry { afterSquaredSecond(sling.prop.long("package.manager.installRetry") ?: 2) }

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    val installRecursive = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.manager.installRecursive")?.let { set(it) }
    }

    /**
     * Deploys only if package is changed (checksum based) or reinstalled on instance in the meantime.
     */
    val deployAvoidance = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.manager.deployAvoidance")?.let { set(it) }
    }

    /**
     * Allows to temporarily enable or disable workflows during CRX package deployment.
     */
    val workflowToggle = sling.obj.map<String, Boolean> {
        convention(mapOf())
        sling.prop.map("package.manager.workflowToggle")?.let { m -> set(m.mapValues { it.value.toBoolean() }) }
    }

    /**
     * Repeat listing package when failed (brute-forcing).
     */
    var listRetry = common.retry { afterSquaredSecond(sling.prop.long("package.manager.listRetry") ?: 3) }

    /**
     * Packages are installed lazy which means already installed will no be installed again.
     * By default, information about currently installed packages is being retrieved from Sling only once.
     *
     * This flag can change that behavior, so that information will be refreshed after each package installation.
     */
    @Input
    val listRefresh = sling.obj.boolean {
        convention(false)
        sling.prop.boolean("package.manager.listRefresh")?.let { set(it) }
    }

    /**
     * Repeat download when failed (brute-forcing).
     */
    var downloadRetry = common.retry { afterSquaredSecond(sling.prop.long("package.manager.downloadRetry") ?: 3) }

    /**
     * Define patterns for known exceptions which could be thrown during package installation
     * making it impossible to succeed.
     *
     * When declared exception is encountered during package installation process, no more
     * retries will be applied.
     */
    val errors = sling.obj.strings {
        convention(listOf(
                "javax.jcr.nodetype.*Exception",
                "org.apache.jackrabbit.oak.api.*Exception",
                "org.apache.jackrabbit.vault.packaging.*Exception",
                "org.xml.sax.*Exception"
        ))
        sling.prop.list("package.manager.errors")?.let { set(it) }
    }

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified. Affects CRX packages composed and satisfied.
     */
    val snapshots = sling.obj.strings {
        convention(listOf())
        sling.prop.list("package.manager.snapshots")?.let { set(it) }
    }

    /**
     * Determines number of lines to process at once during reading Package Manager HTML responses.
     *
     * The higher the value, the bigger consumption of memory but shorter execution time.
     * It is a protection against exceeding max Java heap size.
     */
    val responseBuffer = sling.obj.int {
        convention(4096)
        sling.prop.int("package.manager.responseBuffer")?.let { set(it) }
    }

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

    fun find(path: String) = find { it.firstOrNull { pkg -> pkg.path == path } }

    fun find(group: String, name: String, version: String): Package? = find { it.firstOrNull { pkg -> pkg.compare(group, name, version) } }

    fun findAll(group: String, name: String) = find { it.filter { pkg -> pkg.compare(group, name) } }

    private fun <T> find(resolver: (List<Package>) -> T): T {
        logger.debug("Asking for uploaded packages on $instance")
        return common.buildScope.getOrPut("instance.${instance.name}.packages", { list() }, listRefresh.get()).let(resolver)
    }

    operator fun contains(file: File) = find(file) != null

    fun list(): List<Package> {
        return listRetry.withCountdown<List<Package>, InstanceException>("list packages on '${instance.name}'") {
            return try {
                http.get("/bin/cpm/package.list.json") { asObjectFromJson<Array<Package>>(it) }.toList()
            } catch (e: RequestException) {
                throw InstanceException("Cannot list packages on $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after listing packages on $instance. Cause: ${e.message}", e)
            }
        }
    }

    fun upload(file: File): UploadResponse {
        return uploadRetry.withCountdown<UploadResponse, InstanceException>("upload package '${file.name}' on '${instance.name}'") {
            logger.info("Uploading package '$file' to $instance'")

            val response = try {
                http.postMultipart("/bin/cpm/package.upload.json", mapOf(
                        "file" to file,
                        "force" to (uploadForce.get() || isSnapshot(file)) // TODO is this supported by composum?
                )) { asObjectFromJson<UploadResponse>(it) }
            } catch (e: FileNotFoundException) {
                throw PackageException("Package '$file' to be uploaded not found!", e)
            } catch (e: RequestException) {
                throw InstanceException("Cannot upload package '$file' to $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after uploading package '$file' to $instance. Cause: ${e.message}", e)
            }

            if (!response.success) {
                throw InstanceException("Cannot upload package '$file' to $instance. Reason: ${interpretFail(response.status)}.")
            }

            return response
        }
    }

    /**
     * Create package on the fly, upload it to instance then build it.
     * Next built package is downloaded - replacing initially created package.
     * Finally built package is deleted on instance (preventing messing up).
     */
    fun download(definition: PackageDefinition.() -> Unit) = download(PackageDefinition(sling).apply {
        version.set(Formats.dateFileName())
        definition()
    })

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
                if (path != null) {
                    delete(path)
                }
            }

            return file
        }
    }

    fun download(remotePath: String, targetFile: File = common.temporaryFile(FilenameUtils.getName(remotePath))) {
        return downloadRetry.withCountdown<Unit, InstanceException>("download package '$remotePath' on '${instance.name}'") {
            logger.info("Downloading package from '$remotePath' to file '$targetFile'")

            http.download(remotePath, targetFile)

            if (!targetFile.exists()) {
                throw InstanceException("Downloaded package is missing: ${targetFile.path}")
            }
        }
    }

    fun build(remotePath: String): BuildResponse {
        logger.info("Building package '$remotePath' on $instance")

        val response = try {
            http.postMultipart("/bin/cpm/package.build.json", mapOf("path" to remotePath)) { asObjectFromJson<BuildResponse>(it) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot build package '$remotePath' on $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after building package '$remotePath' on $instance. Cause: ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot build package '$remotePath' on $instance. Cause: ${interpretFail(response.status)}")
        }

        return response
    }

    fun install(file: File) = install(get(file).path)

    fun install(remotePath: String): InstallResponse {
        return installRetry.withCountdown<InstallResponse, InstanceException>("install package '$remotePath' on '${instance.name}'") {
            logger.info("Installing package '$remotePath' on $instance")

            val response = try {
                http.postMultipart("/bin/cpm/package.install.json", mapOf(
                        "path" to remotePath,
                        "recursive" to installRecursive.get()) // TODO it this supported by composum?
                ) { asObjectFromJson<InstallResponse>(it) }
            } catch (e: RequestException) {
                throw InstanceException("Cannot install package '$remotePath' on $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after installing package '$remotePath' on $instance. Cause: ${e.message}", e)
            }

            if (!response.success) {
                throw InstanceException("Cannot install package '$remotePath' on $instance. Status: ${response.status}")
            }

            return response
        }
    }

    private fun interpretFail(message: String): String = when (message) {
        "Inaccessible value" -> "Probably no disk space left (server respond with '$message')"
        else -> message
    }

    fun isSnapshot(file: File): Boolean = Patterns.wildcard(file, snapshots.get())

    fun isDeployed(file: File): Boolean {
        val pkg = find(file) ?: return false
        return isDeployed(pkg)
    }

    fun isDeployed(pkg: Package): Boolean {
        if (!pkg.installed) return false
        val otherVersions = findAll(pkg.definition.group, pkg.definition.name).filter { it != pkg }
        return otherVersions.none { it.definition.installedTimestamp > pkg.definition.installedTimestamp }
    }

    fun deploy(file: File): Boolean {
        if (deployAvoidance.get()) {
            return deployAvoiding(file)
        }

        deployRegularly(file)

        return true
    }

    @OptIn(ExperimentalTime::class)
    private fun deployAvoiding(file: File): Boolean {
        val pkg = find(file)
        val checksumTimed = measureTimedValue { Checksum.md5(file) }
        val checksumLocal = checksumTimed.value

        logger.info("Package '$file' checksum '$checksumLocal' calculation took '${checksumTimed.duration}'")

        if (pkg == null || !isDeployed(pkg)) {
            val pkgPath = deployRegularly(file)
            val pkgMeta = getMetadataNode(pkgPath)

            saveMetadataNode(pkgMeta, checksumLocal, pkgPath)
            return true
        } else {
            val pkgPath = pkg.path
            val pkgMeta = getMetadataNode(pkgPath)
            val checksumRemote = if (pkgMeta.exists) pkgMeta.properties.string(METADATA_CHECKSUM_PROP) else null
            val lastUnpackedPrevious = if (pkgMeta.exists) pkgMeta.properties.date(METADATA_LAST_UNPACKED_PROP) else null
            val lastUnpackedCurrent = readLastUnpacked(pkgPath)

            val checksumChanged = checksumLocal != checksumRemote
            val externallyUnpacked = lastUnpackedPrevious != lastUnpackedCurrent

            if (checksumChanged || externallyUnpacked) {
                if (externallyUnpacked) {
                    logger.warn("Deploying package '$pkgPath' on $instance (changed externally at '$lastUnpackedCurrent')")
                }
                if (checksumChanged) {
                    logger.info("Deploying package '$pkgPath' on $instance (changed checksum)")
                }

                deployRegularly(file)
                saveMetadataNode(pkgMeta, checksumLocal, pkgPath)
                return true
            } else {
                logger.lifecycle("No need to deploy package '$pkgPath' on $instance (no changes)")
                return false
            }
        }
    }

    private fun readLastUnpacked(pkgPath: String): Date {
        return sync.repository.node(pkgPath).child(DEFINITION_PATH).properties.date(METADATA_LAST_UNPACKED_PROP)
                ?: throw PackageException("Cannot read package '$pkgPath' installation time on $instance!")
    }

    private fun getMetadataNode(pkgPath: String) = sync.repository.node("$METADATA_PATH/${Formats.toHashCodeHex(pkgPath)}")

    private fun saveMetadataNode(node: Node, checksum: String, pkgPath: String) = node.save(mapOf(
            Node.TYPE_UNSTRUCTURED,
            METADATA_PATH_PROP to pkgPath,
            METADATA_CHECKSUM_PROP to checksum,
            METADATA_LAST_UNPACKED_PROP to readLastUnpacked(pkgPath)
    ))

    private fun deployRegularly(file: File): String = upload(file).path.also { install(it) }

    fun delete(file: File) = delete(get(file).path)

    fun delete(remotePath: String): DeleteResponse {
        logger.info("Deleting package '$remotePath' on $instance")

        val response = try {
            http.postMultipart("/bin/cpm/package.delete.json", mapOf("path" to remotePath)) { asObjectFromJson<DeleteResponse>(it) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot delete package '$remotePath' from $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after deleting package '$remotePath' from $instance. Cause: ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot delete package '$remotePath' from $instance. Status: ${response.status}.")
        }

        return response
    }

    fun uninstall(file: File) = uninstall(get(file).path)

    fun uninstall(remotePath: String): UninstallResponse {
        logger.info("Uninstalling package '$remotePath' on $instance")

        val response = try {
            http.postMultipart("/bin/cpm/package.install.json", mapOf("path" to remotePath)) { asObjectFromJson<UninstallResponse>(it) }
        } catch (e: RequestException) {
            throw InstanceException("Cannot uninstall package '$remotePath' on $instance. Cause: ${e.message}", e)
        } catch (e: ResponseException) {
            throw InstanceException("Malformed response after uninstalling package '$remotePath' from $instance. Cause ${e.message}", e)
        }

        if (!response.success) {
            throw InstanceException("Cannot uninstall package '$remotePath' from $instance. Status: ${response.status}.")
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
            sling.logger.info(e.message)
            sling.logger.debug("Nothing to purge.", e)
        }
    }

    companion object {
        const val DEFINITION_PATH = "jcr:content/vlt:definition"

        const val METADATA_PATH = "/var/gap/package/deploy"

        const val METADATA_PATH_PROP = "path"

        const val METADATA_CHECKSUM_PROP = "checksumMd5"

        const val METADATA_LAST_UNPACKED_PROP = "lastUnpacked"
    }
}
