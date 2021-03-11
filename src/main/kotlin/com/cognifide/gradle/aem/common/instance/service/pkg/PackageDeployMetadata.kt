package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.common.utils.Formats
import java.util.*

class PackageDeployMetadata(val manager: PackageManager, val pkgPath: String, val checksumLocal: String) {

    val lastUnpackedCurrent: Date = readLastUnpacked()

    val node = manager.sync.repository.node("$STORAGE_PATH/${Formats.toHashCodeHex(pkgPath)}")

    val lastUnpackedPrevious: Date? = node.takeIf { it.exists }?.properties?.date(LAST_UNPACKED_PROP)

    val externallyUnpacked: Boolean = lastUnpackedPrevious != lastUnpackedCurrent

    val checksumRemote: String? = node.takeIf { it.exists }?.properties?.string(CHECKSUM_PROP)

    val checksumUpdated: Boolean = checksumLocal != checksumRemote

    val updated get() = checksumUpdated || externallyUnpacked

    val upToDate get() = !updated

    fun update() = node.save(
        mapOf(
            Node.TYPE_UNSTRUCTURED,
            PATH_PROP to pkgPath,
            CHECKSUM_PROP to checksumLocal,
            LAST_UNPACKED_PROP to readLastUnpacked()
        )
    )

    private fun readLastUnpacked(): Date {
        val properties = manager.sync.repository.node(pkgPath).child(DEFINITION_PATH).properties
        return properties.date(LAST_UNPACKED_PROP)
            ?: properties.date(LAST_UNWRAPPED_PROP)
            ?: throw PackageException("Cannot read package '$pkgPath' installation time on ${manager.instance}!")
    }

    companion object {

        const val DEFINITION_PATH = "jcr:content/vlt:definition"

        const val STORAGE_PATH = "/var/gap/package/deploy"

        const val PATH_PROP = "path"

        const val CHECKSUM_PROP = "checksumMd5"

        const val LAST_UNPACKED_PROP = "lastUnpacked"

        const val LAST_UNWRAPPED_PROP = "lastUnwrapped"
    }
}
