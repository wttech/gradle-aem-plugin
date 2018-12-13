package com.cognifide.gradle.aem.tooling.tasks

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.pkg.PackageDownloader
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Checkout : Vlt() {

    /**
     * Determines a method of getting JCR content from remote instance.
     */
    @Internal
    var type = Type.of(aem.props.string("aem.checkout.type") ?: Type.PACKAGE_DOWNLOAD.name)

    @Internal
    var instance = aem.instanceAny

    @Internal
    var filter = aem.filter

    @Internal
    val downloader = PackageDownloader(aem, AemTask.temporaryDir(project, name))

    init {
        description = "Check out JCR content from running AEM instance."
        outputs.upToDateWhen { false }
    }

    fun download(configurer: PackageDownloader.() -> Unit) {
        downloader.apply(configurer)
    }

    fun useVltCheckout() {
        type = Type.VLT_CHECKOUT
    }

    fun usePackageDownload() {
        type = Type.PACKAGE_DOWNLOAD
    }

    @TaskAction
    override fun perform() {
        when (type) {
            Type.VLT_CHECKOUT -> performVltCheckout()
            Type.PACKAGE_DOWNLOAD -> performPackageDownload()
        }
        aem.notifier.notify("Checked out JCR content", "Instance: ${instance.name}. Directory: ${Formats.rootProjectPath(vlt.contentPath, project)}")
    }

    private fun performVltCheckout() {
        vlt.apply {
            command = "--credentials ${instance.credentials} checkout --force --filter ${filter.file} ${instance.httpUrl}/crx/server/crx.default"
            run()
        }
    }

    private fun performPackageDownload() {
        downloader.apply {
            instance = this@Checkout.instance
            filter = this@Checkout.filter

            download()
        }
    }

    enum class Type {
        VLT_CHECKOUT,
        PACKAGE_DOWNLOAD;

        companion object {
            fun of(name: String): Type {
                return values().find { it.name.equals(name, true) }
                        ?: throw AemException("Unsupported checkout type: $name")
            }
        }
    }

    companion object {
        const val NAME = "aemCheckout"
    }
}