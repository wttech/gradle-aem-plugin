package com.cognifide.gradle.aem.base.tasks

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.pkg.PackageDownloader
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Checkout : Vlt() {

    /**
     * Determines a method of getting JCR content from remote instance.
     */
    @Input
    var type = Type.of(aem.props.string("aem.checkout.type") ?: Type.PACKAGE_DOWNLOAD.name)

    @Input
    var instance = aem.instanceAny

    @Input
    var filter = aem.filter

    @Nested
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
    }

    private fun performVltCheckout() {
        vlt.apply {
            command = "--credentials ${instance.credentials} checkout --force --filter ${filter.file} ${instance.httpUrl}/crx/server/crx.default"
            run()
        }
        aem.notifier.notify("Checked out JCR content", "Instance: ${instance.name}. Directory: ${Formats.rootProjectPath(vlt.contentPath, project)}")
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