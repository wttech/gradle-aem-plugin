package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.TaskAction

open class InstanceDeploy : Instance() {

    val verbose = aem.obj.boolean {
        convention(true)
        common.prop.boolean("instance.deploy.verbose")?.let { set(it) }
    }

    val pkgZip = aem.obj.file {
        common.prop.string("instance.deploy.packageUrl")?.let { url ->
            fileProvider(aem.obj.provider { common.resolveFile(url) })
        }
    }

    val bundleJar = aem.obj.file {
        common.prop.string("instance.deploy.bundleUrl")?.let { url ->
            fileProvider(aem.obj.provider { common.resolveFile(url) })
        }
    }

    @TaskAction
    fun deploy() {
        instanceManager.examine(instances.get())

        when {
            pkgZip.isPresent -> {
                val zip = pkgZip.get().asFile
                instanceManager.fileSync { deployPackage(zip) }
                common.notifier.notify("Package deployed", "${zip.name} on ${instances.get().names}")
            }
            bundleJar.isPresent -> {
                val jar = bundleJar.get().asFile
                instanceManager.fileSync { installBundle(jar) }
                common.notifier.notify("Bundle deployed", "${jar.name} on ${instances.get().names}")
            }
            else -> {
                val msg = "Neither URL of package nor bundle provided so nothing to deploy to instance(s)!"
                if (verbose.get()) {
                    throw InstanceException(msg)
                } else {
                    logger.info(msg)
                }
            }
        }
    }

    init {
        description = "Deploys to instances package or bundle by providing URL, path or dependency notation"
    }

    companion object {
        const val NAME = "instanceDeploy"
    }
}
