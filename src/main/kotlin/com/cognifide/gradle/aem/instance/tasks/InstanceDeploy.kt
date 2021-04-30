package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InstanceDeploy : Instance() {

    @Internal
    val file = aem.obj.file {
        common.prop.string("instance.deploy.url")?.let { url ->
            fileProvider(aem.obj.provider { common.resolveFile(url) })
        }
    }

    @Internal
    val pkgZip = aem.obj.file {
        common.prop.string("instance.deploy.packageUrl")?.let { url ->
            fileProvider(aem.obj.provider { common.resolveFile(url) })
        }
    }

    @Internal
    val bundleJar = aem.obj.file {
        common.prop.string("instance.deploy.bundleUrl")?.let { url ->
            fileProvider(aem.obj.provider { common.resolveFile(url) })
        }
    }

    @TaskAction
    fun deploy() {
        instanceManager.examine(anyInstances)

        when {
            pkgZip.isPresent -> deployPackage(pkgZip.get().asFile)
            bundleJar.isPresent -> deployBundle(bundleJar.get().asFile)
            file.isPresent -> {
                val file = file.get().asFile
                when (file.extension) {
                    "zip" -> deployPackage(file)
                    "jar" -> deployBundle(file)
                    else -> throw InstanceException("File '$file' has unsupported type and cannot be deployed to instance(s)!")
                }
            }
            else -> {
                val msg = "Neither URL of package nor bundle provided so nothing to deploy to instance(s)!"
                if (aem.commonOptions.verbose.get()) {
                    throw InstanceException(msg)
                } else {
                    logger.info(msg)
                }
            }
        }
    }

    private fun deployPackage(zip: File) {
        instanceManager.fileSync { deployPackage(zip) }
        common.notifier.notify("Package deployed", "${zip.name} on ${anyInstances.names}")
    }

    private fun deployBundle(jar: File) {
        instanceManager.fileSync { installBundle(jar) }
        common.notifier.notify("Bundle deployed", "${jar.name} on ${anyInstances.names}")
    }

    init {
        description = "Deploys to instances package or bundle by providing URL, path or dependency notation"
    }

    companion object {
        const val NAME = "instanceDeploy"
    }
}
