package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.api.Project
import org.gradle.api.logging.Logger

open class CheckoutConfig (private val project: Project, private val config: AemConfig) {

    private val logger: Logger = project.logger

    fun determineInstance(instanceConfig: String?): Instance {
        if (!instanceConfig.isNullOrBlank()) {
            val cmdInstance = config.parseInstance(instanceConfig!!)

            logger.info("Using instance specified by command line parameter: $cmdInstance")
            return cmdInstance
        }

        val namedInstance = Instance.filter(project, config.instanceName).firstOrNull()
        if (namedInstance != null) {
            logger.info("Using first instance matching filter '${config.instanceName}': $namedInstance")
            return namedInstance
        }

        val anyInstance = Instance.filter(project, Instance.FILTER_ANY).firstOrNull()
        if (anyInstance != null) {
            logger.info("Using first instance matching filter '${Instance.FILTER_ANY}': $anyInstance")
            return anyInstance
        }

        throw VltException("Vault instance cannot be determined neither by command line parameter nor AEM config.")
    }

    fun determineFilter(filterRoots: List<String>, checkoutFilterPath: String): VltFilter {
        return if (filterRoots.isNotEmpty()) {
            logger.info("Using Vault filter roots specified as command line property: $filterRoots")
            VltFilter.temporary(project, filterRoots)
        } else {
            if (checkoutFilterPath.isNotBlank()) {
                val configFilter = FileOperations.find(project, config.vaultPath, checkoutFilterPath)
                        ?: throw VltException("Vault check out filter file does not exist at path: $checkoutFilterPath (or under directory: ${config.vaultPath}).")
                VltFilter(configFilter)
            } else {
                val conventionFilter = FileOperations.find(project, config.vaultPath, config.checkoutFilterPaths)
                        ?: throw VltException("None of Vault check out filter file does not exist at one of convention paths: ${config.checkoutFilterPaths}.")
                VltFilter(conventionFilter)
            }
        }
    }
}