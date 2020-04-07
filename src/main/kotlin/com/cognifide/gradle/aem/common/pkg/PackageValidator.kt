package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.ZipFile
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.CndSync
import com.cognifide.gradle.aem.common.cli.CliApp
import com.cognifide.gradle.aem.common.pkg.validator.OakpalResult
import com.cognifide.gradle.aem.common.pkg.validator.OakpalSeverity
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.*
import java.io.File
import java.util.jar.Manifest

class PackageValidator(@Internal val aem: AemExtension) {

    private val common = aem.common

    private val logger = aem.logger

    private val app = CliApp(aem).apply {
        dependencyDir.convention(aem.project.layout.buildDirectory.dir("oakpal/cli"))
        dependencyNotation.apply {
            convention(aem.commonOptions.archiveExtension.map { "net.adamcin.oakpal:oakpal-cli:1.5.1:dist@$it" })
            aem.prop.string(("oakpal.cli"))?.let { set(it) }
        }
        executable.set("oakpal-cli-1.5.1/bin/oakpal.sh")
    }

    /**
     * Allows to disable package validation at all.
     */
    @Input
    val enabled = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.validator.enabled")?.let { set(it) }
    }

    /**
     * Controls if failed validation should also fail current build.
     */
    @Input
    val verbose = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.validator.verbose")?.let { set(it) }
    }

    @OutputDirectory
    val workDir = aem.obj.buildDir("package/validator")

    @Internal
    val opearDir = aem.obj.dir { convention(workDir.dir(Package.OAKPAL_OPEAR_PATH)) }

    @Input
    val planName = aem.obj.string {
        convention("plan.json")
        aem.prop.string("package.validator.plan")?.let { set(it) }
    }

    @Input
    val severity = aem.obj.typed<OakpalSeverity> {
        convention(OakpalSeverity.MAJOR)
        aem.prop.string("package.validator.severity")?.let { set(OakpalSeverity.of(it)) }
    }

    fun severity(name: String) {
        severity.set(OakpalSeverity.of(name))
    }

    @Internal
    val reportFile = aem.obj.file { convention(workDir.file("report.json")) }

    @InputFile
    @Optional
    val baseFile = aem.obj.file()

    fun base(value: Any) = base { common.resolveFile(value) }

    fun base(provider: () -> File) {
        baseFile.fileProvider(aem.obj.provider(provider))
    }

    @Internal
    val configDir = aem.obj.relativeDir(aem.packageOptions.commonDir, "validator/${Package.OAKPAL_OPEAR_PATH}")

    @Internal
    val initialDir = aem.obj.relativeDir(aem.packageOptions.commonDir, "validator/initial")

    @Internal
    val initialPkg = aem.obj.file { convention(workDir.file("initial.zip")) }

    @Internal
    val cndSync = CndSync(aem).apply {
        aem.prop.string("package.validator.cndSync.type")?.let { type(it) }
        file.apply {
            convention(initialDir.file("META-INF/vault/nodetypes.cnd"))
            aem.prop.file("package.validator.cndSync.file")?.let { set(it) }
        }
    }

    fun perform(vararg packages: File) = perform(packages.toList())

    fun perform(packages: Iterable<File>) = perform(packages.toList())

    fun perform(packages: Collection<File>) {
        if (packages.isEmpty()) {
            logger.info("No packages provided for validation.")
            return
        }

        logger.info("Packages provided for validation:\n${packages.joinToString("\n")}")

        if (!enabled.get()) {
            logger.info("Packages validation is skipped as of validator is disabled!")
            return
        }

        common.progress {
            step = "Synchronizing node types"
            cndSync.sync()

            step = "Preparing resources"
            prepareWorkDir()

            step = "Validating"
            message = when (packages.size) {
                1 -> "Package '${packages.first().name}'"
                else -> "Packages (${packages.count()})"
            }
            runOakPal(packages)
        }
    }

    private fun prepareWorkDir() {
        workDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        if (initialDir.get().asFile.exists()) {
            logger.info("Preparing OakPAL initial package '${initialPkg.get()}'")
            aem.composePackage {
                archivePath.set(initialPkg)
                filter("/var/gap/package/validator") // anything
                content { FileUtils.copyDirectory(initialDir.get().asFile, pkgDir) }
            }
        }

        logger.info("Preparing OakPAL Opear directory '${opearDir.get()}'")

        val tmpDir = opearDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        FileOperations.copyResources(Package.OAKPAL_OPEAR_RESOURCES_PATH, tmpDir)

        baseFile.orNull?.asFile?.let { file ->
            logger.info("Extracting OakPAL Opear base configuration files from '$file' to directory '$tmpDir'")
            ZipFile(file).unpackAll(tmpDir)
        }

        configDir.get().asFile.takeIf { it.exists() }?.let { dir ->
            logger.info("Using project-specific OakPAL Opear configuration files from directory '$dir' to '$tmpDir'")
            FileUtils.copyDirectory(dir, tmpDir)
        }

        val manifestFile = opearDir.file(Package.MANIFEST_PATH).get().asFile
        if (manifestFile.exists()) {
            Manifest(manifestFile.readBytes().inputStream()).apply {
                mainAttributes.putValue("Oakpal-Plan", planName.get())
                manifestFile.outputStream().use { write(it) }
            }
        }
    }

    @Suppress("SpreadOperator")
    private fun runOakPal(packages: Collection<File>) {
        val allPackages = mutableListOf<File>().apply {
            if (initialPkg.get().asFile.exists()) {
                add(initialPkg.get().asFile)
            }
            addAll(packages)
        }

        val result = OakpalResult.byExitCode(app.exec {
            isIgnoreExitValue = true
            environment("OAKPAL_OPEAR", opearDir.get().asFile.absolutePath)
            workingDir(workDir.get().asFile)
            args("-p", planName.get(), "-s", severity.get(), "-j", "-o", reportFile.get().asFile, *allPackages.toTypedArray())
        }.exitValue)
        if (result != OakpalResult.SUCCESS) {
            val message = "OakPAL validation failed due to ${result.cause}!\nSee report file: '${reportFile.get()}' for package(s):\n" +
                    packages.joinToString("\n")
            if (verbose.get()) {
                throw PackageException(message)
            } else {
                logger.error(message)
            }
        }
    }

    // TODO https://github.com/gradle/gradle/issues/2016
    @InputFiles
    val inputFiles = aem.obj.files {
        from(configDir)
        from(initialDir)
    }

    init {
        aem.packageOptions.validatorOptions(this)
        aem.prop.string("package.validator.base")?.let { base(it) }
    }
}
