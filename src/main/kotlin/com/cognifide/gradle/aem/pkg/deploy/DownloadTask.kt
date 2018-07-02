package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemNotifier
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileContentReader
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip
import java.io.File

open class DownloadTask : Zip(), AemTask {

    @Nested
    final override val config = AemConfig.of(project)

    @InputDirectory
    val rootDir = AemTask.temporaryDir(project, PrepareTask.NAME)

    @Internal
    val propertyParser = PropertyParser(project)

    @InputFile
    var filterFile = File(propertyParser.string("aem.download.filterPath", config.vaultFilterPath))

    @get:Internal
    val instance
        get() = Instance.filter(project).first()

    @get:Internal
    val compose
        get() = project.tasks.getByName(ComposeTask.NAME) as ComposeTask


    @TaskAction
    override fun copy() {
        super.copy()
        uploadPackage()
        buildPackage()
    }

    fun uploadPackage() {
        InstanceSync(project, instance).uploadPackage(archivePath)
    }

    fun buildPackage() {

        InstanceSync(project, instance).buildPackage(archiveName)
//        InstanceSync(project, instance).determineRemotePackage(archivePath, false)
    }


    init {
        description = "Downloads CRX package from JCR content"
        group = AemTask.GROUP

        baseName = config.packageName
        duplicatesStrategy = DuplicatesStrategy.WARN
        isZip64 = true
        classifier = CLASSIFIER_DEFAULT

        includeVault()

        outputs.upToDateWhen { false }
        doLast {
            AemNotifier.of(project).default("Package downloaded", getArchiveName())
        }
        project.afterEvaluate {
            if(!filterFile.exists()) {
                throw AemException("Unable to download without param '-Paem.download.filterPath'")
            }
        }
    }

    fun includeVault() {
        into("", { spec ->
            spec.from(rootDir)
            exclude("**/filter.xml")
            spec.eachFile {  FileContentReader.filter(it, { propertyParser.expandPackage(it, mapOf(), path) }) }
        })
        into(PackagePlugin.VLT_PATH, { spec ->
            spec.from(filterFile)
            rename(filterFile.name, "filter.xml")
            spec.eachFile {  FileContentReader.filter(it, { propertyParser.expandPackage(it, mapOf(), path) }) }
        })

    }

    companion object {
        const val NAME = "aemDownload"
        const val CLASSIFIER_DEFAULT = "download"
    }
}