package com.cognifide.gradle.aem.instance.service.pkg

import com.cognifide.gradle.aem.common.BuildScope
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceService
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.pkg.*
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import java.io.File
import java.io.FileNotFoundException
import org.apache.commons.io.FilenameUtils
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.zeroturnaround.zip.ZipUtil

/**
 * Allows to communicate with CRX Package Manager.
 *
 * @see <https://helpx.adobe.com/experience-manager/6-5/sites/administering/using/package-manager.html>
 */
class PackageManager(sync: InstanceSync) : InstanceService(sync) {

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

    fun resolvePackage(compose: PackageCompose, refresh: Boolean = true, retry: Retry = aem.retry()): Package? {
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
        return retry.withCountdown<ListResponse, InstanceException>("list packages") {
            return try {
                sync.postMultipart(LIST_JSON) { asObjectFromJson(it, ListResponse::class.java) }
            } catch (e: RequestException) {
                throw InstanceException("Cannot list packages on $instance. Reason: request failed.", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after listing packages on $instance.", e)
            }
        }
    }

    fun uploadPackage(file: File, force: Boolean = true, retry: Retry = aem.retry()): UploadResponse {
        return retry.withCountdown<UploadResponse, InstanceException>("upload package") {
            val url = "$JSON_PATH/?cmd=upload"

            aem.logger.info("Uploading package $file to $instance'")

            val response = try {
                sync.postMultipart(url, mapOf(
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
    }

    /**
     * Create package on the fly, upload it to instance then build it.
     * Finally download built package by replacing it with initially created.
     */
    fun downloadPackage(definition: PackageDefinition.() -> Unit, retry: Retry): File {
        return retry.withCountdown<File, InstanceException>("download package") {
            val file = aem.composePackage {
                version = "download" // prevents CRX package from task 'Compose' being replaced
                definition()
            }

            var path: String? = null
            try {
                val pkg = uploadPackage(file)
                file.delete()

                path = pkg.path
                buildPackage(path)

                downloadPackage(path, file)
            } finally {
                if (path != null) {
                    deletePackage(path)
                }
            }

            return file
        }
    }

    fun downloadPackage(definition: PackageDefinition.() -> Unit) = downloadPackage(definition, aem.retry())

    fun downloadPackage(remotePath: String, targetFile: File = aem.temporaryFile(FilenameUtils.getName(remotePath)), retry: Retry = aem.retry()) {
        return retry.withCountdown<Unit, InstanceException>("download package") {
            aem.logger.info("Downloading package from $remotePath to file $targetFile")

            sync.download(remotePath, targetFile)

            if (!targetFile.exists()) {
                throw InstanceException("Downloaded package is missing: ${targetFile.path}")
            }
        }
    }

    fun buildPackage(remotePath: String): PackageBuildResponse {
        val url = "$JSON_PATH$remotePath/?cmd=build"

        aem.logger.info("Building package $remotePath on $instance")

        val response = try {
            sync.postMultipart(url) { asObjectFromJson(it, PackageBuildResponse::class.java) }
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
        return retry.withCountdown<InstallResponse, InstanceException>("install package") {
            val url = "$HTML_PATH$remotePath/?cmd=install"

            aem.logger.info("Installing package $remotePath on $instance")

            val response = try {
                sync.postMultipart(url, mapOf("recursive" to recursive)) { InstallResponse.from(asStream(it), aem.packageOptions.responseBuffer) }
            } catch (e: RequestException) {
                throw InstanceException("Cannot install package $remotePath on $instance. Reason: request failed.", e)
            } catch (e: ResponseException) {
                throw InstanceException("Malformed response after installing package $remotePath on $instance.")
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
        // https://forums.adobe.com/thread/2338290
        "Inaccessible value" -> "no disk space left (server respond with '$message'})"
        else -> message
    }

    fun isSnapshot(file: File): Boolean {
        return Patterns.wildcard(file, aem.packageOptions.snapshots)
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
        val url = "$JSON_PATH$remotePath/?cmd=replicate"

        aem.logger.info("Activating package $remotePath on $instance")

        val response = try {
            sync.postMultipart(url) { asObjectFromJson(it, UploadResponse::class.java) }
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
        val url = "$HTML_PATH$remotePath/?cmd=delete"

        aem.logger.info("Deleting package $remotePath on $instance")

        val response = try {
            sync.postMultipart(url) { DeleteResponse.from(asStream(it), aem.packageOptions.responseBuffer) }
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
        val url = "$HTML_PATH$remotePath/?cmd=uninstall"

        aem.logger.info("Uninstalling package using command: $url")

        val response = try {
            sync.postMultipart(url) { UninstallResponse.from(asStream(it), aem.packageOptions.responseBuffer) }
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

    companion object {
        const val PATH = "/crx/packmgr/service"

        const val JSON_PATH = "$PATH/.json"

        const val HTML_PATH = "$PATH/.html"

        const val LIST_JSON = "/crx/packmgr/list.jsp"
    }
}