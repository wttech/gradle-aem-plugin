package com.cognifide.gradle.aem.common.pkg

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

    /**
     * Allows to disable package validation at all.
     */
    @Input
    val enabled = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.validator.enabled")?.let { set(it) }
    }

    /**
     * Determines which level of validation message is indicating failed validation.
     */
    @Input
    val severity = aem.obj.typed<Violation.Severity> {
        convention(Violation.Severity.MAJOR)
        aem.prop.string("package.validator.severity")?.let { set(severityByName(it)) }
    }

    fun severity(name: String) {
        severity.set(severityByName(name))
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

    @Input
    val planName = aem.obj.string {
        convention("default-plan.json")
        aem.prop.string("package.validator.plan")?.let { set(it) }
    }

    @Internal
    val planFile = aem.obj.file {
        convention(workDir.map { dir ->
            dir.file(planName).get().takeIf { it.asFile.exists() } ?: dir.file("plan.json")
        })
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

    // TODO https://github.com/gradle/gradle/issues/2016
    @Internal
    val configDir = aem.obj.relativeDir(aem.packageOptions.configDir, Package.OAKPAL_OPEAR_PATH)

    @InputFiles
    val configFiles = aem.obj.files { from(configDir) }

    @Input
    val jcrNamespaces = aem.obj.map<String, String> { convention(mapOf("crx" to "http://www.day.com/crx/1.0")) }

    @Input
    val jcrPrivileges = aem.obj.strings { convention(listOf("crx:replicate")) }

    @Internal
    val cndFiles = aem.obj.files { from(workDir.file("nodetypes.cnd")) }

    fun oakMachine(options: OakMachine.Builder.() -> Unit) {
        this.oakMachineOptions = options
    }

    private var oakMachineOptions: OakMachine.Builder.() -> Unit = { defaultOptions() }

    private fun OakMachine.Builder.defaultOptions() {
        withInitStage(InitStage.Builder().apply {
            cndFiles.filter { it.exists() }.forEach { withOrderedCndUrl(it.toURI().toURL()) }
            jcrNamespaces.get().forEach { (prefix, uri) -> withNs(prefix, uri) }
            withPrivileges(jcrPrivileges.get())
        }.build())
    }

    fun planClassLoader(provider: () -> ClassLoader) {
        this.planClassLoaderProvider = provider
    }

    private var planClassLoaderProvider: () -> ClassLoader = { javaClass.classLoader }

    fun perform(vararg packages: File) = perform(packages.asIterable())

    fun perform(packages: Iterable<File>) {
        logger.info("Considered CRX packages in validation:${packages.joinToString("\n")}")

        if (!enabled.get()) {
            logger.info("Validation of CRX packages is skipped as of validator is disabled!")
            return
        }

        common.progress {
            message = when (packages.count()) {
                1 -> "Validating CRX package '${packages.first().name}'"
                else -> "Validating CRX packages (${packages.count()})"
            }

            syncNodeTypes()
            prepareOpearDir()
            runOakPal(packages)
        }
    }

    private fun syncNodeTypes() {
        // TODO ...
    }

    /*
        private fun syncNodeTypes() {
        when (vaultNodeTypesSync.get()) {
            NodeTypesSync.ALWAYS -> syncNodeTypesOrElse {
                throw PackageException("Cannot synchronize node types because none of AEM instances are available!")
            }
            NodeTypesSync.AUTO -> syncNodeTypesOrFallback()
            NodeTypesSync.PRESERVE_AUTO -> {
                if (!vaultNodeTypesSyncFile.get().asFile.exists()) {
                    syncNodeTypesOrFallback()
                }
            }
            NodeTypesSync.FALLBACK -> syncNodeTypesFallback()
            NodeTypesSync.PRESERVE_FALLBACK -> {
                if (!vaultNodeTypesSyncFile.get().asFile.exists()) {
                    syncNodeTypesFallback()
                }
            }
            NodeTypesSync.NEVER -> {}
            null -> {}
        }
    }

    fun syncNodeTypesOrElse(action: () -> Unit) = common.buildScope.doOnce("syncNodeTypes") {
        aem.availableInstance?.sync {
            try {
                vaultNodeTypesSyncFile.get().asFile.apply {
                    parentFile.mkdirs()
                    writeText(crx.nodeTypes)
                }
            } catch (e: CommonException) {
                aem.logger.debug("Cannot synchronize node types using $instance! Cause: ${e.message}", e)
                action()
            }
        } ?: action()
    }

    fun syncNodeTypesOrFallback() = syncNodeTypesOrElse {
        aem.logger.debug("Using fallback instead of synchronizing node types (forced or AEM instances are unavailable).")
        syncNodeTypesFallback()
    }

    fun syncNodeTypesFallback() = vaultNodeTypesSyncFile.get().asFile.writeText(nodeTypeFallback)

     */

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
            throw PackageException("OakPAL Opear directory cannot be read properly!")
        }

        val plan = determinePlan(opearFile)

        logger.info("Validating CRX packages using OakPAL plan '$plan'")

        val scanResult = OakpalPlan.fromJson(plan)
                .map { p ->
                    p.toOakMachineBuilder(DefaultErrorListener(), opearFile.getPlanClassLoader(planClassLoaderProvider())).apply {
                        withNodeStoreSupplier { MemoryNodeStore() }
                        oakMachineOptions()
                    }.build()
                }
                .map { oak -> oak.scanPackages(packages.toList()) }

        if (scanResult.isFailure) {
            val e = scanResult.error.get()
            throw PackageException("Validating CRX packages aborted due to OAKPal failure! Cause: '${e.message}'", e)
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
        logger.info("Saving OAKPal reports to file '${reportFile.get().asFile}' for CRX package(s):\n${packages.joinToString("\n")}")

        try {
            ReportMapper.writeReportsToFile(reports, reportFile.get().asFile)
        } catch (e: IOException) {
            throw PackageException("Cannot save OAKPal reports to '${reportFile.get().asFile}'! Cause: '${e.message}'", e)
        }
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    private fun analyzeReports(packages: Iterable<File>, reports: List<CheckReport>) {
        val violatedReports = reports.filter { it.violations.isNotEmpty() }
        val shouldFail = violatedReports.any { !it.getViolations(severity.get()).isEmpty() }

        val violationLogger = CollectingLogger()
        var violationSeverityReached = 0

        if (violatedReports.isNotEmpty()) {
            violationLogger.info("OakPAL check violations for CRX package(s):\n${packages.joinToString("\n")}")

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
                    " severity '${severity.get()}' for CRX package(s):\n${packages.joinToString("\n")}!"

            if (verbose.get()) {
                throw PackageException(failMessage)
            } else {
                logger.error(failMessage)
            }
        } else {
            violationLogger.logTo(logger)
        }
    }

    private fun severityByName(name: String): Violation.Severity {
        return Violation.Severity.values().firstOrNull { it.name.equals(name, true) }
                ?: throw PackageException("Unsupported package violation severity specified '$name'!")
    }

    init {
        aem.packageOptions.validatorOptions(this)
        aem.prop.string("package.validator.base")?.let { base(it) }
    }
}
