package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemNotifier
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.CheckoutConfig
import com.cognifide.gradle.aem.base.vlt.VltException
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileContentReader
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip

open class DownloadTask : Zip(), AemTask {

    companion object {
        const val NAME = "aemDownload"
        const val CLASSIFIER_DOWNLOAD = "download"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @InputDirectory
    val prepareDir = AemTask.taskDir(project, PrepareTask.NAME)

    @OutputDirectory
    val downloadDir = AemTask.temporaryDir(project, NAME, CLASSIFIER_DOWNLOAD)

    @Internal
    val props = PropertyParser(project)

    private val checkoutConfig = CheckoutConfig(project, props, config)

    private val checkoutFilter by lazy { checkoutConfig.determineCheckoutFilter()}

    private val instance by lazy { checkoutConfig.determineCheckoutInstance()}

    @TaskAction
    override fun copy() {
        //Prepare shell package
        super.copy()

        val sync = InstanceSync(project, instance)

        val packagePath = sync.uploadPackage(archivePath).path
        sync.buildPackage(packagePath)
        sync.downloadPackage(packagePath, downloadDir)
    }

    init {
        description = "Downloads CRX package from JCR content"
        group = AemTask.GROUP

        baseName = config.packageName
        duplicatesStrategy = DuplicatesStrategy.WARN
        isZip64 = true
        classifier = "$CLASSIFIER_DOWNLOAD-shell"
        destinationDir = downloadDir
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        //Load zip configuration only when the task is in the graph
        project.gradle.taskGraph.whenReady {
            if (it.hasTask(this)) {
                this.specShellPackage()
            }
        }

        // Task is always executed when in the graph
        outputs.upToDateWhen { false }

        doLast {
            AemNotifier.of(project).default("Package downloaded", getArchiveName())
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