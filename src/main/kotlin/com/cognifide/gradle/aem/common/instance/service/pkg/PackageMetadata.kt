package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.common.utils.Formats
import java.util.*

@Suppress("TooGenericExceptionCaught")
class PackageMetadata(val manager: PackageManager, val pkgPath: String) {

    private val logger = manager.logger

    val node by lazy { manager.sync.repository.node("$STORAGE_PATH/${Formats.toHashCodeHex(pkgPath)}") }

    val checksumRemote: String? by lazy { node.takeIf { it.exists }?.properties?.string(CHECKSUM_PROP) }

    fun validChecksum(checksumLocal: String) = (checksumRemote != null) && (checksumLocal == checksumRemote)

    val installedCurrent: Date? by lazy { readInstalledDate() }

    val installedPrevious: Date? get() = node.takeIf { it.exists }?.properties?.date(INSTALLED_PROP)

    val installedExternally: Boolean get() = (installedCurrent != null) && (installedPrevious != null) && (installedCurrent != installedPrevious)

    fun update(checksumLocal: String, action: () -> Unit) = try {
        maybeSaveChecksum(checksumLocal)
        action()
        maybeSaveInstalledDate()
    } catch (e: Exception) {
        maybeRestoreChecksum()
        throw e
    }

    private fun maybeSaveChecksum(checksumLocal: String) = try {
        save(mapOf(CHECKSUM_PROP to checksumLocal))
    } catch (e: Exception) {
        logger.debug("Cannot save checksum '$checksumLocal' of package '$pkgPath'!", e)
    }

    private fun maybeRestoreChecksum() = try {
        save(mapOf(CHECKSUM_PROP to checksumRemote))
    } catch (e: Exception) {
        logger.debug("Cannot restore checksum '$checksumRemote' of package '$pkgPath'!", e)
    }

    private fun maybeSaveInstalledDate() = try {
        save(mapOf(INSTALLED_PROP to readInstalledDate()))
    } catch (e: Exception) {
        logger.debug("Cannot save last installed date for package '$pkgPath'!", e)
    }

    private fun readInstalledDate(): Date? {
        val properties = manager.sync.repository.node(pkgPath).child(DEFINITION_PATH).properties
        return properties.date(LAST_UNPACKED_PROP) ?: properties.date(LAST_UNWRAPPED_PROP)
    }

    private fun save(props: Map<String, Any?>) = node.save(
        mapOf(
            Node.TYPE_UNSTRUCTURED,
            PATH_PROP to pkgPath
        ) + props
    )

    fun examineDeployment(checksum: String) = PackageDeployment(this, checksum)

    companion object {

        const val STORAGE_PATH = "/var/gap/package/deploy"

        const val PATH_PROP = "path"

        const val CHECKSUM_PROP = "checksumMd5"

        const val INSTALLED_PROP = "installed"

        const val DEFINITION_PATH = "jcr:content/vlt:definition"

        const val LAST_UNPACKED_PROP = "lastUnpacked"

        const val LAST_UNWRAPPED_PROP = "lastUnwrapped"
    }
}
