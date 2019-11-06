package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.CollectingLogger
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import net.adamcin.oakpal.core.*
import org.apache.commons.io.FileUtils
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException
import java.net.URL

class PackageValidator(@Internal val aem: AemExtension) {

    private val logger = aem.logger

    @Input
    var enabled = aem.props.boolean("package.validator.enabled") ?: true

    @Input
    var severity = aem.props.string("package.validator.severity")
            ?.let { severityByName(it) } ?: Violation.Severity.MAJOR

    fun severity(name: String) {
        this.severity = severityByName(name)
    }

    @Input
    var verbose = aem.props.boolean("package.validator.verbose") ?: true

    @OutputDirectory
    var workDir = aem.temporaryDir("package/validator")

    @Input
    var planName = aem.props.string("package.validator.plan") ?: "default-plan.json"

    @get:Internal
    val planFile get() = File(workDir, planName)

    @get:Internal
    val reportFile: File
        get() = File(workDir, "report.json")

    private var baseProvider: () -> File? = {
        aem.props.string("package.validator.opear.base")?.let { aem.resolveFile(it) }
    }

    fun base(value: Any) = base { aem.resolveFile(value) }

    fun base(provider: () -> File?) {
        this.baseProvider = provider
    }

    @get:InputFile
    @get:Optional
    val baseFile: File? get() = baseProvider()

    @get:InputFiles
    val configDirs get() = _configDirs.filter { it.exists() }

    private var _configDirs = mutableListOf(
            File(aem.configCommonDir, CONFIG_DIR_PATH),
            File(aem.configDir, CONFIG_DIR_PATH)
    )

    fun configDir(dir: File) {
        _configDirs.add(dir)
    }

    fun configDirs(dirs: Iterable<File>) {
        _configDirs = dirs.toMutableList()
    }

    fun configDirs(vararg dirs: File) = configDirs(dirs.asIterable())

    private var classLoaderProvider: () -> ClassLoader = { javaClass.classLoader }

    fun classLoader(provider: () -> ClassLoader) {
        this.classLoaderProvider = provider
    }

    fun perform(vararg packages: File) = perform(packages.asIterable())

    fun perform(packages: Iterable<File>) {
        prepareOpearDir()
        runOakPal(packages)
    }

    private fun prepareOpearDir() {
        logger.info("Preparing OakPAL Opear directory '$workDir'")

        workDir.deleteRecursively()
        workDir.mkdirs()

        FileOperations.copyResources(Package.OAKPAL_OPEAR_RESOURCES_PATH, workDir)

        baseFile?.let { file ->
            logger.info("Extracting OakPAL Opear base configuration files from '$file' to directory '$workDir'")
            FileOperations.zipUnpackAll(file, workDir)
        }

        configDirs.filter { it.exists() }.forEach { configDir ->
            logger.info("Using project-specific OakPAL Opear configuration files from directory '$configDir' to '$workDir'")

            FileUtils.copyDirectory(configDir, workDir)
        }
    }

    private fun runOakPal(packages: Iterable<File>) {
        val opearFile = OpearFile.fromDirectory(workDir).getOrElse {
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
            throw PackageException("Cannot validate CRX package(s) '${listPackages(packages)}' due to internal OAKPal failure!")
        } else {
            val reports = scanResult.getOrDefault(emptyList<CheckReport>())
            saveReports(packages, reports)
            analyzeReports(packages, reports)
        }
    }

    private fun determinePlan(opearFile: OpearFile): URL {
        if (planFile.exists()) {
            return planFile.toURI().toURL()
        }

        return opearFile.getSpecificPlan(planName).getOrDefault(opearFile.defaultPlan)
    }

    private fun saveReports(packages: Iterable<File>, reports: List<CheckReport>?) {
        logger.info("Saving OAKPal reports for CRX package(s) '${listPackages(packages)}' to file '$reportFile'")

        try {
            ReportMapper.writeReportsToFile(reports, reportFile)
        } catch (e: IOException) {
            throw PackageException("Cannot save OAKPal reports for CRX package(s) '${listPackages(packages)}'! Cause: '${e.message}'", e)
        }
    }

    @Suppress("ComplexMethod")
    private fun analyzeReports(packages: Iterable<File>, reports: List<CheckReport>) {
        val violatedReports = reports.filter { !it.violations.isEmpty() }
        val shouldFail = violatedReports.any { !it.getViolations(severity).isEmpty() }

        val violationLogger = CollectingLogger()
        var violationSeverityReached = 0

        violationLogger.info("OakPAL check results for CRX package(s) '${listPackages(packages)}':")

        violatedReports.forEach { report ->
            if (report.violations.isNotEmpty())  {
                violationLogger.info("  ${report.checkName}")

                for (violation in report.violations) {
                    val packageIds = violation.packages.map { it.downloadName }
                    val violationLog = when {
                        packageIds.isNotEmpty() -> "    <${violation.severity}> ${violation.description} $packageIds"
                        else -> "    <${violation.severity}> ${violation.description}"
                    }
                    if (violation.severity.isLessSevereThan(severity)) {
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
                    " severity '$severity' for CRX package(s) '${listPackages(packages)}'!"

            if (verbose) {
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
    }

    companion object {
        const val CONFIG_DIR_PATH = "package/OAKPAL_OPEAR"
    }
}
