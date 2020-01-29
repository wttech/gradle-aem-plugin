package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.common.build.Retry
import com.cognifide.gradle.common.http.RequestException
import com.cognifide.gradle.common.http.ResponseException
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import java.io.File
import java.io.FileNotFoundException
import org.apache.commons.io.FilenameUtils

/**
 * Allows to communicate with CRX Package Manager.
 *
 * @see <https://helpx.adobe.com/experience-manager/6-5/sites/administering/using/package-manager.html>
 */
class PackageManager(sync: InstanceSync) : InstanceService(sync) {

    private val http = sync.http

    fun get(file: File, refresh: Boolean = true, retry: Retry = common.retry()): Package {
        if (!file.exists()) {
            throw PackageException("Package $file does not exist so it cannot be resolved on $instance")
        }

        return find(file, refresh, retry)
                ?: throw InstanceException("Package is not uploaded on $instance")
    }

    fun get(group: String, name: String, version: String, refresh: Boolean = true, retry: Retry = common.retry()): Package {
        return find(group, name, version, refresh, retry)
                ?: throw InstanceException("Package ${Package.coordinates(group, name, version)}' is not uploaded on $instance")
    }

    fun find(file: File, refresh: Boolean = true, retry: Retry = common.retry()): Package? = PackageFile(file).run {
        find(group, name, version, refresh, retry)
    }

    fun find(group: String, name: String, version: String, refresh: Boolean = true, retry: Retry = common.retry()): Package? {
        return find({ listResponse ->
            val expected = Package(group, name, version)

            logger.info("Finding package '${expected.coordinates}' on $instance")
            val actual = listResponse.resolvePackage(expected)
            if (actual == null) {
                logger.info("Package not found '${expected.coordinates}' on $instance")
            }

            actual
        }, refresh, retry)
    }

    private fun find(resolver: (ListResponse) -> Package?, refresh: Boolean, retry: Retry = common.retry()): Package? {
        logger.debug("Asking for uploaded packages on $instance")

        return resolver(common.buildScope.getOrPut("instance.${instance.name}.packages", { list(retry) }, refresh))
    }

    fun list(retry: Retry = common.retry()): ListResponse {
        return retry.withCountdown<ListResponse, InstanceException>("list packages on '${instance.name}'") {
            return try {
                http.postMultipart(LIST_JSON) { asObjectFromJson(it, ListResponse::class.java) }
            } catch (e: RequestException) {
                throw InstanceException("Cannot list packages on $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after listing packages on $instance. Cause: ${e.message}", e)
            }
        }
    }

    fun upload(file: File, force: Boolean = true, retry: Retry = common.retry()): UploadResponse {
        return retry.withCountdown<UploadResponse, InstanceException>("upload package '${file.name}' on '${instance.name}'") {
            val url = "$JSON_PATH/?cmd=upload"

            logger.info("Uploading package $file to $instance'")

            val response = try {
                http.postMultipart(url, mapOf(
                        "package" to file,
                        "force" to (force || isSnapshot(file))
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
    fun download(definition: PackageDefinition.() -> Unit, retry: Retry): File {
        val file = aem.composePackage {
            version = Formats.dateFileName()
            definition()
        }

        return retry.withCountdown<File, InstanceException>("download package '${file.name}' on '${instance.name}'") {
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

    fun download(definition: PackageDefinition.() -> Unit) = download(definition, common.retry())

    fun download(
        remotePath: String,
        targetFile: File = common.temporaryFile(FilenameUtils.getName(remotePath)),
        retry: Retry = common.retry()
    ) {
        return retry.withCountdown<Unit, InstanceException>("download package '$remotePath' on '${instance.name}'") {
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

    fun install(remotePath: String, recursive: Boolean = true, retry: Retry = common.retry()): InstallResponse {
        return retry.withCountdown<InstallResponse, InstanceException>("install package '$remotePath' on '${instance.name}'") {
            val url = "$HTML_PATH$remotePath/?cmd=install"

            logger.info("Installing package $remotePath on $instance")

            val response = try {
                http.postMultipart(url, mapOf("recursive" to recursive)) { InstallResponse.from(asStream(it), aem.packageOptions.responseBuffer) }
            } catch (e: RequestException) {
                throw InstanceException("Cannot install package $remotePath on $instance. Cause: ${e.message}", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after installing package $remotePath on $instance. Cause: ${e.message}", e)
            }

            if (response.hasPackageErrors(aem.packageOptions.errors)) {
                throw PackageException("Cannot install malformed package $remotePath on $instance. Status: ${response.status}. Errors: ${response.errors}")
            } else if (!response.success) {
                throw InstanceException("Cannot install package $remotePath on $instance. Status: ${response.status}. Errors: ${response.errors}")
            }

            return response
        }
    }

    private fun interpretFail(message: String): String = when (message) {
        "Inaccessible value" -> "no disk space left (server respond with '$message'})" // https://forums.adobe.com/thread/2338290
        else -> message
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, aem.packageOptions.snapshots)
    }

    @Suppress("LongParameterList")
    fun deploy(
        file: File,
        uploadForce: Boolean = true,
        uploadRetry: Retry = common.retry(),
        installRecursive: Boolean = true,
        installRetry: Retry = common.retry(),
        activate: Boolean = false
    ) {
        val uploadResponse = upload(file, uploadForce, uploadRetry)
        val packagePath = uploadResponse.path

        install(packagePath, installRecursive, installRetry)
        if (activate) {
            activate(packagePath)
        }
    }

    fun distribute(
        file: File,
        uploadForce: Boolean = true,
        uploadRetry: Retry = common.retry(),
        installRecursive: Boolean = true,
        installRetry: Retry = common.retry()
    ) = deploy(file, uploadForce, uploadRetry, installRecursive, installRetry, true)

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
            http.postMultipart(url) { DeleteResponse.from(asStream(it), aem.packageOptions.responseBuffer) }
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
            http.postMultipart(url) { UninstallResponse.from(asStream(it), aem.packageOptions.responseBuffer) }
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
