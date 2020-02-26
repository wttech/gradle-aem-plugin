package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.common.CommonException

class CndSync(private val aem: AemExtension) {

    private val common = aem.common

    val file = aem.obj.file()

    val type = aem.obj.typed<CndSyncType> { convention(CndSyncType.PRESERVE) }

    fun type(name: String) {
        type.set(CndSyncType.of(name))
    }

    fun sync() {
        val file = file.get().asFile
        val unavailableMessage by lazy { "Cannot synchronize CND file '$file' because none of AEM instances are available!" }

        when (type.get()) {
            CndSyncType.ALWAYS -> syncOrElse { throw PackageException(unavailableMessage) }
            CndSyncType.PRESERVE -> {
                if (!file.exists()) {
                    syncOrElse { aem.logger.warn(unavailableMessage) }
                }
            }
            CndSyncType.NEVER -> {}
            null -> {}
        }
    }

    private fun syncOrElse(action: () -> Unit) = common.buildScope.doOnce("cndSync") {
        aem.availableInstance?.sync {
            try {
                file.get().asFile.apply {
                    parentFile.mkdirs()
                    writeText(crx.nodeTypes)
                }
            } catch (e: CommonException) {
                aem.logger.debug("Cannot synchronize CND file using $instance! Cause: ${e.message}", e)
                action()
            }
        } ?: action()
    }
}
