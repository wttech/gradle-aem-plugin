package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.base.vlt.VltCleaner
import com.cognifide.gradle.aem.internal.file.FileException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ExtractPackageTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemDownloadExtract"
        const val FORCE_NEW_FLAG = "extract.force.new"
        const val SKIP_CLEANUP = "extract.skip.cleanup"
    }

    @Input
    val packageForceNew = props.flag(FORCE_NEW_FLAG)

    @Input
    val skipCleanup = props.flag(SKIP_CLEANUP)

    @Internal
    val vltCleaner = VltCleaner(project)

    init {
        description = "Extracts the contents of package downloaded using ${DownloadTask.NAME} task"
    }

    @TaskAction
    fun extract() {
        val ext = project.extensions.extraProperties
        if (ext.has(DownloadTask.DOWNLOADED_PACKAGE_PROPERTY)) {
            val downloadedPackage = ext.get(DownloadTask.DOWNLOADED_PACKAGE_PROPERTY)
            val jcrRoot = prepareJcrRoot()
            extractContents(downloadedPackage as File, jcrRoot)
            if(!skipCleanup) {
                vltCleaner.clean(jcrRoot)
            }

        } else {
            throw FileException("Missing package downloaded by ${DownloadTask.NAME} task")
        }
    }

    private fun extractContents(downloadedPackage: File, jcrRoot: File) {
        project.copy { spec ->
            spec.into(jcrRoot.parentFile.path)
                    .from(project.zipTree(downloadedPackage.path))
                    .include("${PackagePlugin.JCR_ROOT}/**")

        }
    }

    private fun prepareJcrRoot(): File {
        val content = File(config.contentPath)
        val jcrRoot = File(content, PackagePlugin.JCR_ROOT)

        if(!skipCleanup) {
            vltCleaner.prepare(jcrRoot)
        }

        if (jcrRoot.exists() && packageForceNew) {
            //TODO add logging
            jcrRoot.deleteRecursively()
        }

        jcrRoot.mkdirs()
        return jcrRoot
    }
}