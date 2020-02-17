package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.common.build.CollectingLogger
import net.adamcin.oakpal.core.*
import org.apache.commons.io.FileUtils
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException
import java.net.URL

class PackageValidator(@Internal val aem: AemExtension) {

    private val common = aem.common

    private val logger = aem.logger

    @Input
    val enabled = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.validator.enabled")?.let { set(it) }
    }

    @Input
    val severity = aem.obj.typed<Violation.Severity> {
        convention(Violation.Severity.MAJOR)
        aem.prop.string("package.validator.severity")?.let { set(severityByName(it)) }
    }

    fun severity(name: String) {
        severity.set(severityByName(name))
    }

    @Input
    val verbose = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.validator.verbose")?.let { set(it) }
    }

    @OutputDirectory
    val workDir = aem.obj.buildDir("package/validator")

    @Input
    val planName = aem.obj.string {
        convention("default-plan.json")
        aem.prop.string("package.validator.plan")?.let { set(it) }
    }

    @Internal
    val planFile = aem.obj.file { convention(workDir.file(planName)) }

    @Internal
    val reportFile = aem.obj.file { convention(workDir.file("report.json")) }

    @InputFile
    @Optional
    val baseFile = aem.obj.file()

    fun base(value: Any) = base { common.resolveFile(value) }

    fun base(provider: () -> File) {
        baseFile.fileProvider(aem.obj.provider(provider))
    }

    // TODO https://github.com/gradle/gradle/issues/2016
    @Internal
    val configDir = aem.obj.relativeDir(aem.packageOptions.configDir, Package.OAKPAL_OPEAR_PATH)

    @InputFiles
    val configFiles = aem.obj.files { from(configDir) }

    private var classLoaderProvider: () -> ClassLoader = { javaClass.classLoader }

    fun classLoader(provider: () -> ClassLoader) {
        this.classLoaderProvider = provider
    }

    fun perform(vararg packages: File) = perform(packages.asIterable())

    fun perform(packages: Iterable<File>) {
        if (!enabled.get()) {
            logger.info("Validating CRX packages(s) '${listPackages(packages)}' skipped as of validator is disabled.")
            return
        }

        common.progress {
            message = "Validating CRX package(s) '${packages.joinToString(", ") { it.name }}'"

            prepareOpearDir()
            runOakPal(packages)
        }
    }

    private fun prepareOpearDir() {
        logger.info("Preparing OakPAL Opear directory '${workDir.get()}'")

        val workDir = workDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        workDir.deleteRecursively()
        workDir.mkdirs()

        FileOperations.copyResources(Package.OAKPAL_OPEAR_RESOURCES_PATH, workDir)

        baseFile.orNull?.asFile?.let { file ->
            logger.info("Extracting OakPAL Opear base configuration files from '$file' to directory '$workDir'")
            FileOperations.zipUnpackAll(file, workDir)
        }

        configDir.get().asFile.takeIf { it.exists() }?.let { dir ->
            logger.info("Using project-specific OakPAL Opear configuration files from directory '$dir' to '$workDir'")
            FileUtils.copyDirectory(dir, workDir)
        }
    }

    private fun runOakPal(packages: Iterable<File>) {
        val opearFile = OpearFile.fromDirectory(workDir.get().asFile).getOrElse {
            throw AemException("OakPAL Opear directory cannot be read properly!")
        }

        val plan = determinePlan(opearFile)

        logger.info("Validating CRX packages(s) '${listPackages(packages)}' using OakPAL plan '$plan'")

        val scanResult = OakpalPlan.fromJson(plan)
                .map { p ->
                    p.toOakMachineBuilder(DefaultErrorListener(), opearFile.getPlanClassLoader(classLoaderProvider()))
                            .withNodeStoreSupplier { MemoryNodeStore() }
                            .build()
                }
                .map { oak -> oak.scanPackages(packages.toList()) }

        if (scanResult.isFailure) {
            val e = scanResult.error.get()
            throw PackageException("Cannot validate CRX package(s) '${listPackages(packages)}' due to OAKPal failure! Cause: '${e.message}'", e)
        } else {
            val reports = scanResult.getOrDefault(emptyList<CheckReport>())
            saveReports(packages, reports)
            analyzeReports(packages, reports)
        }
    }

    private fun determinePlan(opearFile: OpearFile): URL {
        val planFile = planFile.get().asFile
        if (planFile.exists()) {
            return planFile.toURI().toURL()
        }

        return opearFile.getSpecificPlan(planName.get()).getOrDefault(opearFile.defaultPlan)
    }

    private fun saveReports(packages: Iterable<File>, reports: List<CheckReport>?) {
        logger.info("Saving OAKPal reports for CRX package(s) '${listPackages(packages)}' to file '${reportFile.get().asFile}'")

        try {
            ReportMapper.writeReportsToFile(reports, reportFile.get().asFile)
        } catch (e: IOException) {
            throw PackageException("Cannot save OAKPal reports for CRX package(s) '${listPackages(packages)}'! Cause: '${e.message}'", e)
        }
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    private fun analyzeReports(packages: Iterable<File>, reports: List<CheckReport>) {
        val violatedReports = reports.filter { it.violations.isNotEmpty() }
        val shouldFail = violatedReports.any { !it.getViolations(severity.get()).isEmpty() }

        val violationLogger = CollectingLogger()
        var violationSeverityReached = 0

        if (violatedReports.isNotEmpty()) {
            violationLogger.info("OakPAL check violations for CRX package(s) '${listPackages(packages)}':")

            violatedReports.forEach { report ->
                violationLogger.info("  ${report.checkName}")

                for (violation in report.violations) {
                    val packageIds = violation.packages.map { it.downloadName }
                    val violationLog = when {
                        packageIds.isNotEmpty() -> "    <${violation.severity}> ${violation.description} $packageIds"
                        else -> "    <${violation.severity}> ${violation.description}"
                    }
                    if (violation.severity.isLessSevereThan(severity.get())) {
                        violationLogger.info(violationLog)
                    } else {
                        violationSeverityReached++
                        violationLogger.error(violationLog)
                    }
                }
            }
        }

        if (shouldFail) {
            violationLogger.logTo(logger) { level ->
                when (level) {
                    LogLevel.INFO -> LogLevel.LIFECYCLE
                    else -> level
                }
            }

            val failMessage = "OAKPal check violations ($violationSeverityReached) were reported at or above" +
                    " severity '${severity.get()}' for CRX package(s) '${listPackages(packages)}'!"

            if (verbose.get()) {
                throw PackageException(failMessage)
            } else {
                logger.error(failMessage)
            }
        } else {
            violationLogger.logTo(logger)
        }
    }

    private fun listPackages(files: Iterable<File>) = files.joinToString(", ")

    private fun severityByName(name: String): Violation.Severity {
        return Violation.Severity.values().firstOrNull { it.name.equals(name, true) }
                ?: throw PackageException("Unsupported package violation severity specified '$name'!")
    }

    init {
        aem.packageOptions.validatorOptions(this)
        aem.prop.string("package.validator.opear.base")?.let { base(it) }
    }
}
