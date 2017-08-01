package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.deploy.DeploySynchronizer
import org.gradle.api.Project

class BundleRepository(val project: Project, val sync: DeploySynchronizer) {

    val instance = sync.instance

    val logger = project.logger

    fun ask(): BundleState {
        return try {
            BundleState.fromJson(sync.get(sync.bundlesUrl))
        } catch (e: Exception) {
            logger.debug("Instance bundles asking error", e)
            BundleState.unknown(e)
        }
    }

}