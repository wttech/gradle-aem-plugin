package com.cognifide.gradle.sling.common.pkg

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.asset.AssetManager
import com.cognifide.gradle.sling.common.file.ZipFile
import com.cognifide.gradle.sling.common.instance.service.pkg.Package
import com.cognifide.gradle.sling.common.pkg.vault.CndSync
import com.cognifide.gradle.sling.common.cli.CliApp
import com.cognifide.gradle.sling.common.pkg.validator.OakpalResult
import com.cognifide.gradle.sling.common.pkg.validator.OakpalSeverity
import com.cognifide.gradle.common.utils.using
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.*
import java.io.File

class PackageValidator(@Internal val sling: SlingExtension) {

    private val common = sling.common

    private val logger = sling.logger

    private val cli = CliApp(sling).apply {
        dependencyNotation.apply {
            convention("net.adamcin.oakpal:oakpal-cli:2.0.0:dist")
            sling.prop.string("oakpal.cli.dependency")?.let { set(it) }
        }
        executable.apply {
            convention("oakpal-cli-2.0.0/bin/oakpal")
            sling.prop.string("oakpal.cli.executable")?.let { set(it) }
        }
    }

    fun cli(options: CliApp.() -> Unit) = cli.using(options)

    /**
     * Allows to disable package validation at all.
     */
    @Input
    val enabled = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.validator.enabled")?.let { set(it) }
    }

    /**
     * Controls if failed validation should also fail current build.
     */
    @Input
    val verbose = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.validator.verbose")?.let { set(it) }
    }

    @OutputDirectory
    val workDir = sling.obj.buildDir("package/validator")

    @Internal
    val opearDir = sling.obj.dir { convention(workDir.dir(Package.OAKPAL_OPEAR_PATH)) }

    @Input
    val planName = sling.obj.string {
        convention("plan.json")
        sling.prop.string("package.validator.plan")?.let { set(it) }
    }

    @Internal
    val planFile = sling.obj.file { convention(sling.obj.provider { opearDir.file(planName.get()).get() }) }

    @Input
    val severity = sling.obj.typed<OakpalSeverity> {
        convention(OakpalSeverity.MAJOR)
        sling.prop.string("package.validator.severity")?.let { set(OakpalSeverity.of(it)) }
    }

    fun severity(name: String) {
        severity.set(OakpalSeverity.of(name))
    }

    @Internal
    val reportFile = sling.obj.file { convention(workDir.file("report.json")) }

    @InputFile
    @Optional
    val baseFile = sling.obj.file()

    fun base(value: Any) = base { common.resolveFile(value) }

    fun base(provider: () -> File) {
        baseFile.fileProvider(sling.obj.provider(provider))
    }

    @Internal
    val configDir = sling.obj.relativeDir(sling.packageOptions.commonDir, "validator/${Package.OAKPAL_OPEAR_PATH}")

    @Internal
    val initialDir = sling.obj.relativeDir(sling.packageOptions.commonDir, "validator/initial")

    @Internal
    val initialPkg = sling.obj.file { convention(workDir.file("initial.zip")) }

    @Internal
    val cndSync = CndSync(sling).apply {
        sling.prop.string("package.validator.cndSync.type")?.let { type(it) }
        file.apply {
            convention(initialDir.file("META-INF/vault/nodetypes.cnd"))
            sling.prop.file("package.validator.cndSync.file")?.let { set(it) }
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

            step = "Preparing initial package"
            prepareInitialPackage()

            step = "Preparing Opear directory"
            prepareOpearDir()

            step = "Running OakPAL"
            message = when (packages.size) {
                1 -> "Package '${packages.first().name}'"
                else -> "Packages (${packages.count()})"
            }
            runOakPal(packages)
        }
    }

    private fun prepareOpearDir() {
        logger.info("Preparing OakPAL Opear directory '${opearDir.get()}'")

        val tmpDir = opearDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        sling.assetManager.copyDir(AssetManager.OAKPAL_OPEAR_PATH, tmpDir)

        baseFile.orNull?.asFile?.let { file ->
            logger.info("Extracting OakPAL Opear base configuration files from '$file' to directory '$tmpDir'")
            ZipFile(file).unpackAll(tmpDir)
        }

        configDir.get().asFile.takeIf { it.exists() }?.let { dir ->
            logger.info("Using project-specific OakPAL Opear configuration files from directory '$dir' to '$tmpDir'")
            FileUtils.copyDirectory(dir, tmpDir)
        }
    }

    private fun prepareInitialPackage() {
        logger.info("Preparing OakPAL initial package '${initialPkg.get()}'")

        val tmpDir = workDir.dir("initial").get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        sling.assetManager.copyDir(AssetManager.OAKPAL_INITIAL, tmpDir)

        if (initialDir.get().asFile.exists()) {
            FileUtils.copyDirectory(initialDir.get().asFile, tmpDir)
        }

        sling.composePackage {
            archivePath.set(initialPkg)
            filter("/var/gap/package/validator") // anything
            content { FileUtils.copyDirectory(tmpDir, pkgDir) }
        }

        tmpDir.deleteRecursively()
    }

    @Suppress("SpreadOperator")
    private fun runOakPal(packages: Collection<File>) {
        val result = OakpalResult.byExitCode(cli.exec {
            isIgnoreExitValue = true
            environment("OAKPAL_OPEAR", opearDir.get().asFile.absolutePath)
            workingDir(workDir.get().asFile)
            args("-pi", initialPkg.get().asFile, "-pf", planFile.get().asFile, "-s", severity.get(),
                    "-j", "-o", reportFile.get().asFile, *packages.toTypedArray())
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
    val inputFiles = sling.obj.files {
        from(configDir)
        from(initialDir)
    }

    init {
        sling.packageOptions.validatorOptions(this)
        sling.prop.string("package.validator.base")?.let { base(it) }
    }
}
