package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.asset.AssetManager
import com.cognifide.gradle.aem.common.file.ZipFile
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.CndSync
import com.cognifide.gradle.aem.common.cli.CliApp
import com.cognifide.gradle.aem.common.pkg.validator.OakpalResult
import com.cognifide.gradle.aem.common.pkg.validator.OakpalSeverity
import com.cognifide.gradle.common.utils.using
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.*
import java.io.File
import java.util.jar.Manifest

class PackageValidator(@Internal val aem: AemExtension) {

    private val common = aem.common

    private val logger = aem.logger

    private val cli = CliApp(aem).apply {
        dependencyNotation.apply {
            convention("net.adamcin.oakpal:oakpal-cli:1.5.2:dist")
            aem.prop.string("oakpal.cli.dependency")?.let { set(it) }
        }
        executable.apply {
            convention("oakpal-cli-1.5.2/bin/oakpal")
            aem.prop.string("oakpal.cli.executable")?.let { set(it) }
        }
    }

    fun cli(options: CliApp.() -> Unit) = cli.using(options)

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

        aem.assetManager.copyDir(AssetManager.OAKPAL_OPEAR_PATH, tmpDir)

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

    private fun prepareInitialPackage() {
        logger.info("Preparing OakPAL initial package '${initialPkg.get()}'")

        val tmpDir = workDir.dir("initial").get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        aem.assetManager.copyDir(AssetManager.OAKPAL_INITIAL, tmpDir)

        if (initialDir.get().asFile.exists()) {
            FileUtils.copyDirectory(initialDir.get().asFile, tmpDir)
        }

        aem.composePackage {
            archivePath.set(initialPkg)
            filter("/var/gap/package/validator") // anything
            content { FileUtils.copyDirectory(tmpDir, pkgDir) }
        }

        tmpDir.deleteRecursively()
    }

    @Suppress("SpreadOperator")
    private fun runOakPal(packages: Collection<File>) {
        val allPackages = mutableListOf<File>().apply {
            add(initialPkg.get().asFile)
            addAll(packages)
        }
        val result = OakpalResult.byExitCode(cli.exec {
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
