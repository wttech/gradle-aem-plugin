package com.cognifide.gradle.aem.common.instance.service.pkg

/**
 * Eagerly computes all information needed to examine if package should be (re)deployed or not.
 */
class PackageDeployment(val metadata: PackageMetadata, checksum: String) {

    val installedExternally = metadata.installedExternally

    val installedExternallyAt = metadata.installedCurrent

    val checksumChanged = !metadata.validChecksum(checksum)

    val needed get() = checksumChanged || installedExternally
}
