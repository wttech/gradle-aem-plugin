package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.CheckoutConfig
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileContentReader
import com.cognifide.gradle.aem.internal.file.FileException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip
import java.io.File

open class DownloadTask : Zip(), AemTask {

    companion object {
        const val NAME = "aemDownload"
        const val CLASSIFIER_DOWNLOAD = "download"
        const val CLASSIFIER_SHELL = "shell"
        const val EXTRACT_FLAG = "aem.download.extract"
        const val FORCE_NEW_FLAG = "aem.download.extract.force.new"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @InputDirectory
    private val prepareDir = AemTask.taskDir(project, PrepareTask.NAME)

    @Internal
    val props = PropertyParser(project)

    @Input
    val forceNewFlag = props.flag(FORCE_NEW_FLAG)

    @Input
    val extractFlag = props.flag(EXTRACT_FLAG)

    private val checkoutConfig = CheckoutConfig(project, props, config)

    private val checkoutFilter by lazy { checkoutConfig.determineCheckoutFilter()}

    private val instance by lazy { checkoutConfig.determineCheckoutInstance()}

    init {
        description = "Builds and downloads CRX package from remote instance"
        group = AemTask.GROUP

        baseName = config.packageName
        isZip64 = true

        //Empty package uploaded to aem will have 'shell' suffix
        classifier = CLASSIFIER_SHELL

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
        logger.lifecycle("Uploading package $archivePath")
        val packagePath = sync.uploadPackage(archivePath).path
        logger.lifecycle("Building remote package $packagePath")
        sync.buildPackage(packagePath)
        logger.lifecycle("Downloading remote package $packagePath")
        val downloaded = sync.downloadPackage(packagePath, destinationDir)

        if(downloaded.exists()) {
            if(extractFlag){
                val jcrRoot = prepareJcrRoot()
                extractContents(downloaded, jcrRoot)
            }
        } else {
            throw FileException("Downloaded package missing: ${downloaded.path}")
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
                    //Updating version string for package to contain '-download' suffix
                    props.expandPackage(it, mapOf("project.version" to "${project.version}-$CLASSIFIER_DOWNLOAD"), path)
                }
            }
        }
    }

    private fun extractContents(downloadedPackage: File, jcrRoot: File) {
        logger.lifecycle("Extracting contents of ${downloadedPackage.path} into ${jcrRoot.path}")
        project.copy { spec ->
            spec.into(jcrRoot.parentFile.path)
                    .from(project.zipTree(downloadedPackage.path))
                    .include("${PackagePlugin.JCR_ROOT}/**")
        }
    }

    private fun prepareJcrRoot(): File {
        val content = File(config.contentPath)
        val jcrRoot = File(content, PackagePlugin.JCR_ROOT)

        if (jcrRoot.exists() && forceNewFlag) {
            logger.lifecycle("Deleting contents of ${jcrRoot.path}")
            jcrRoot.deleteRecursively()
        }

        jcrRoot.mkdirs()
        return jcrRoot
    }
}