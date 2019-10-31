package com.cognifide.gradle.aem.common.pkg.validator

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.pkg.PackageException
import net.adamcin.oakpal.core.*
import net.adamcin.oakpal.core.OakpalPlan as BasePlan
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import java.io.File

class PackageValidator(internal val aem: AemExtension) {

    private val logger = aem.logger

    var enabled = aem.props.boolean("aem.package.validator.enabled") ?: true

    var verbose = aem.props.boolean("aem.package.validator.verbose") ?: true

    var reportFile = aem.project.file("build/aem/package/validator/oakpal-report.json")

    private var opearOptions: OpearDir.() -> Unit = {}

    fun opear(options: OpearDir.() -> Unit) {
        this.opearOptions = options
    }

    fun validate(pkg: File, options: OpearDir.() -> Unit = opearOptions) {
        validate(pkg, OpearDir(this).apply(options))
    }

    fun validate(pkg: File, opearDir: OpearDir) {
        opearDir.prepare()

        val opearFile = OpearFile.fromDirectory(opearDir.root).getOrElse {
            throw AemException("Opear directory cannot be read properly!")
        }

        val planUsed = opearDir.plan.takeIf { it.exists() }?.toURI()?.toURL() ?: opearFile.defaultPlan
        val scanResult = BasePlan.fromJson(planUsed)
                .map { plan ->
                    plan.toOakMachineBuilder(DefaultErrorListener(), opearFile.getPlanClassLoader(javaClass.classLoader))
                            .withNodeStoreSupplier { MemoryNodeStore() }
                            .build()
                }
                .map { oak -> oak.scanPackage(pkg) }

        if (scanResult.isFailure) {
            throw PackageException("Validating CRX package '$pkg' ended with failure!")
        } else {
            val reports = scanResult.getOrDefault(emptyList<CheckReport>())
            ReportMapper.writeReportsToFile(reports, reportFile)

            // TODO fail build on errors etc
        }
    }
}
