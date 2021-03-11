package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.common.utils.Formats

class PackageMetadata(val manager: PackageManager, val pkgPath: String) {

    private val logger = manager.logger

    val node by lazy { manager.sync.repository.node("${STORAGE_PATH}/${Formats.toHashCodeHex(pkgPath)}") }

    val checksum: String? by lazy { node.takeIf { it.exists }?.properties?.string(CHECKSUM_PROP) }

    fun validChecksum(value: String) = (checksum != null) && (value == checksum)

    @Suppress("TooGenericExceptionCaught")
    fun updateChecksum(value: String, action: () -> Unit) = try {
        save(mapOf(CHECKSUM_PROP to value))
        action()
    } catch (e: Exception) {
        try {
            save(mapOf(CHECKSUM_PROP to checksum))
        } catch (e: Exception) {
            logger.debug("Cannot restore checksum '$checksum' of package '$pkgPath'!", e)
        }
        throw e
    }

    private fun save(props: Map<String, Any?>) = node.save(mapOf(
        Node.TYPE_UNSTRUCTURED,
        PATH_PROP to pkgPath
    ) + props)

    companion object {

        const val STORAGE_PATH = "/var/gap/package/deploy"

        const val PATH_PROP = "path"

        const val CHECKSUM_PROP = "checksumMd5"
    }
}
