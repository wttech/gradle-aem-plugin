package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InstanceDeploy : Instance() {

    @Internal
    val files = aem.obj.files {
        common.prop.list("instance.deploy.url")?.let { urls ->
            setFrom(aem.obj.provider { common.resolveFiles(urls) })
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

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    @TaskAction
    fun deploy() {
        instanceManager.examine(anyInstances)
        instanceManager.awaitUp(anyInstances, awaitUpOptions)

        if (files.isEmpty && !pkgZip.isPresent && !bundleJar.isPresent) {
            val msg = "Neither URL of package nor bundle provided so nothing to deploy to instance(s)!"
            if (!aem.commonOptions.verbose.get()) logger.info(msg) else throw InstanceException(msg)
        }

        files.forEach {
            when (it.extension) {
                "zip" -> deployPackage(it)
                "jar" -> deployBundle(it)
                else -> {
                    val msg =
                        "File '$it' has unsupported type ('${it.extension}') and cannot be deployed to instance(s)!"
                    if (!aem.commonOptions.verbose.get()) logger.info(msg) else throw InstanceException(msg)
                }
            }
        }

        if (pkgZip.isPresent) deployPackage(pkgZip.get().asFile)
        if (bundleJar.isPresent) deployBundle(bundleJar.get().asFile)
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
