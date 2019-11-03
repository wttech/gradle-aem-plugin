package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import net.adamcin.oakpal.core.*
import org.apache.commons.io.FileUtils
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.gradle.api.tasks.*
import java.io.File

class PackageValidator(val aem: AemExtension) {

    private val logger = aem.logger

    @Input
    var enabled = aem.props.boolean("aem.package.validator.enabled") ?: true

    @Input
    var severity = aem.props.string("package.validator.severity")
            ?.let { severity(it) } ?: Violation.Severity.MAJOR

    @Input
    var verbose = aem.props.boolean("aem.package.validator.verbose") ?: true

    @OutputDirectory
    var root = File(aem.project.buildDir, "aem/package/validator")

    @Input
    var planName = aem.props.string("package.validator.opear.plan") ?: "plan.json"

    @get:Internal
    val planFile get() = File(root, planName)

    @get:Internal
    val reportFile: File
        get() = File(root, "oakpal-report.json")

    private var baseProvider: () -> File? = {
        aem.props.string("package.validator.opear.base")?.let { aem.resolveFile(it) }
    }

    fun base(value: String) = base { aem.resolveFile(value) }

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

    fun prepare() {
        logger.info("Preparing OakPAL Opear directory '$root'")

        root.deleteRecursively()
        root.mkdirs()

        baseFile?.let { file ->
            logger.info("Extracting OakPAL Opear base configuration files from '$file' to directory '$root'")
            FileOperations.zipUnpackAll(file, root)
        }

        configDirs.filter { it.exists() }.forEach {configDir ->
            logger.info("Using project-specific OakPAL Opear configuration files from file '$configDir' to directory '$root'")

            FileUtils.copyDirectory(configDir, root)
        }
    }

    fun perform(vararg packages: File) = perform(packages.asIterable())

    fun perform(packages: Iterable<File>) {
        prepare()

        val opearFile = OpearFile.fromDirectory(root).getOrElse {
            throw AemException("OakPAL Opear directory cannot be read properly!")
        }

        val planUsed = planFile.takeIf { it.exists() }?.toURI()?.toURL() ?: opearFile.defaultPlan

        logger.info("Validating CRX packages(s) '${packages.names}' using OakPAL plan '$planUsed'")

        val scanResult = OakpalPlan.fromJson(planUsed)
                .map { plan ->
                    plan.toOakMachineBuilder(DefaultErrorListener(), opearFile.getPlanClassLoader(javaClass.classLoader))
                            .withNodeStoreSupplier { MemoryNodeStore() }
                            .build()
                }
                .map { oak -> oak.scanPackages(packages.toList()) }

        if (scanResult.isFailure) {
            throw PackageException("Cannot validate CRX package(s) '${packages.names}' due to internal OAKPal failure!")
        } else {
            val reports = scanResult.getOrDefault(emptyList<CheckReport>())
            saveReports(reports)
            analyzeReports(reports)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun saveReports(reports: List<CheckReport>?) {
        try {
            ReportMapper.writeReportsToFile(reports, reportFile)
        } catch (e: Exception) {
            throw PackageException("Cannot save OAKPal reports! Cause: '${e.message}'", e)
        }
    }

    private fun analyzeReports(reports: List<CheckReport>) {
        val violatedReports = reports.filter { !it.violations.isEmpty() }
        val shouldFail = violatedReports.any { !it.getViolations(severity).isEmpty() }

        if (violatedReports.isEmpty()) {
            logger.info("OakPAL checks passed properly!")
            return
        }

        logger.info("OakPAL check results:")

        violatedReports.forEach { report ->
            logger.info("  ${report.checkName}")
            for (violation in report.violations) {
                val packageIds = violation.packages.map { it.downloadName }
                val violationLog = when {
                    packageIds.isNotEmpty() -> "   +- <${violation.severity}> ${violation.description} $packageIds"
                    else -> "   +- <${violation.severity}> ${violation.description}"
                }
                if (violation.severity.isLessSevereThan(severity)) {
                    logger.info(" $violationLog")
                } else {
                    logger.error("" + violationLog)
                }
            }
        }

        val errorMessage = "OAKPal check violations were reported at or above severity '$severity'!"
        if (verbose && shouldFail) {
            throw PackageException(errorMessage)
        } else {
            logger.error(errorMessage)
        }
    }

    private val Iterable<File>.names get() = this.joinToString(", ") { it.name }

    private fun severity(name: String): Violation.Severity {
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
