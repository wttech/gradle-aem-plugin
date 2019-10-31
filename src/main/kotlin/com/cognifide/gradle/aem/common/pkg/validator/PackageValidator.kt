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

    private var opearOptions: OpearOptions.() -> Unit = {}

    fun opear(options: OpearOptions.() -> Unit) {
        this.opearOptions = options
    }

    fun validate(pkg: File, options: OpearOptions.() -> Unit = opearOptions) {
        validate(pkg, OpearOptions(this).apply(options))
    }

    private fun validate(pkg: File, opearDir: OpearOptions) {
        validate(pkg, opearDir.run { prepare(); dir })
    }

    fun validate(pkg: File, opearDir: File) {
        val opear = OpearFile.fromDirectory(opearDir).getOrElse {
            throw AemException("Opear directory cannot be read properly!")
        }

        val scanResult = BasePlan.fromJson(opear.defaultPlan)
                .map { plan ->
                    plan.toOakMachineBuilder(DefaultErrorListener(), opear.getPlanClassLoader(javaClass.classLoader))
                            .withNodeStoreSupplier { MemoryNodeStore() }
                            .build()
                }
                .map { oak -> oak.scanPackage(pkg) }

        if (scanResult.isFailure) {
            throw PackageException("Validating CRX package '$pkg' ended with failure!")
        } else {
            val reports = scanResult.getOrDefault(emptyList<CheckReport>())
            ReportMapper.writeReportsToFile(reports, reportFile)
        }
    }
}
