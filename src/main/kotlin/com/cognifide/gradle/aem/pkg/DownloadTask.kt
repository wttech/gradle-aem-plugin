package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemNotifier
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.CheckoutConfig
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileContentReader
import com.cognifide.gradle.aem.pkg.DownloadTask.Companion.CLASSIFIER_DOWNLOAD
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip
import java.io.File

open class DownloadTask : Zip(), AemTask {

    companion object {
        const val NAME = "aemDownload"
        const val CLASSIFIER_DOWNLOAD = "download"
        const val DOWNLOADED_PACKAGE_PROPERTY = "packageDownloaded"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @InputDirectory
    private val prepareDir = AemTask.taskDir(project, PrepareTask.NAME)

    @OutputDirectory
    private val downloadDir = AemTask.temporaryDir(project, NAME, CLASSIFIER_DOWNLOAD)

    @Internal
    val props = PropertyParser(project)

    private val checkoutConfig = CheckoutConfig(project, props, config)

    private val checkoutFilter by lazy { checkoutConfig.determineCheckoutFilter()}

    private val instance by lazy { checkoutConfig.determineCheckoutInstance()}

    init {
        description = "Builds and downloads CRX package from remote instance"
        group = AemTask.GROUP

        baseName = config.packageName
        isZip64 = true
        classifier = "$CLASSIFIER_DOWNLOAD-shell"
        destinationDir = downloadDir

        //Take only first filter.xml file from definition
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        //Load zip configuration only when the task is in the graph
        project.gradle.taskGraph.whenReady {
            if (it.hasTask(this)) {
                this.specShellPackage()
            }
        }

        // Task is always executed when in the graph
        outputs.upToDateWhen { false }
    }

    @TaskAction
    override fun copy() {
        //Prepare shell package
        super.copy()

        val sync = InstanceSync(project, instance)
        val packagePath = sync.uploadPackage(archivePath).path
        sync.buildPackage(packagePath)
        val downloaded = sync.downloadPackage(packagePath, downloadDir)

        if(downloaded.exists()) {
            project.extensions.extraProperties.set(DOWNLOADED_PACKAGE_PROPERTY, downloaded)
        }
    }

    private fun specShellPackage() {
        into(PackagePlugin.VLT_PATH) { spec ->
            spec.from(checkoutFilter.file)
            rename(checkoutFilter.file.name, "filter.xml")
        }
        into("") { spec ->
            spec.from(prepareDir)
            spec.eachFile {
                FileContentReader.filter(it) {
                    props.expandPackage(it, mapOf("project.version" to "${project.version}-$CLASSIFIER_DOWNLOAD"), path)
                }
            }
        }
    }


}