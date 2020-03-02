package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.common.CommonException

class CndSync(private val aem: AemExtension) {

    private val common = aem.common

    val file = aem.obj.file()

    val type = aem.obj.typed<CndSyncType> {
        convention(aem.obj.provider {
            when {
                aem.commonOptions.offline.get() -> CndSyncType.NEVER
                else -> CndSyncType.PRESERVE
            }
        })
    }

    fun type(name: String) {
        type.set(CndSyncType.of(name))
    }

    fun sync() {
        val file = file.orNull?.asFile
                ?: throw PackageException("CND file to be synchronized is not specified!")
        when (type.get()) {
            CndSyncType.ALWAYS -> syncOrElse {
                throw PackageException("CND file '$file' cannot be synchronized as of none of AEM instances are available!")
            }
            CndSyncType.PRESERVE -> {
                if (!file.exists()) syncOrElse {
                    aem.logger.warn("CND file '$file' does not exist and moreover cannot be synchronized as of none of AEM instances are available!")
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
