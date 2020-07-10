package com.cognifide.gradle.sling.common.pkg.vault

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.pkg.PackageException
import com.cognifide.gradle.common.CommonException

class CndSync(private val sling: SlingExtension) {

    private val common = sling.common

    val file = sling.obj.file()

    val type = sling.obj.typed<CndSyncType> {
        convention(sling.obj.provider {
            when {
                sling.commonOptions.offline.get() -> CndSyncType.NEVER
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
                throw PackageException("CND file '$file' cannot be synchronized as of none of Sling instances are available!")
            }
            CndSyncType.PRESERVE -> {
                if (!file.exists()) syncOrElse {
                    sling.logger.warn("CND file '$file' does not exist and moreover cannot be synchronized as of none of Sling instances are available!")
                }
            }
            CndSyncType.NEVER -> {}
            null -> {}
        }
    }

    private fun syncOrElse(action: () -> Unit) = common.buildScope.doOnce("cndSync") {
        sling.availableInstance?.sync {
            try {
                file.get().asFile.apply {
                    parentFile.mkdirs()
                    writeText(crx.nodeTypes)
                }
            } catch (e: CommonException) {
                sling.logger.debug("Cannot synchronize CND file using $instance! Cause: ${e.message}", e)
                action()
            }
        } ?: action()
    }
}
