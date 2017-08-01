package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.deploy.BundleState
import com.cognifide.gradle.aem.deploy.DeploySynchronizer
import org.gradle.api.Project

class BundleRepository(val project: Project, val sync: DeploySynchronizer) {

    val logger = project.logger

    fun ask(): BundleState? {
        try {
            return BundleState.fromJson(sync.get(sync.bundlesUrl))
        } catch (e: Exception) {
            logger.debug("Instance bundles asking error", e)

            return null
        }
    }

}