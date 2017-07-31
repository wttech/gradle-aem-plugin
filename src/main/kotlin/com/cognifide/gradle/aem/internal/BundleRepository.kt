package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.deploy.BundleResponse
import com.cognifide.gradle.aem.deploy.DeploySynchronizer
import org.gradle.api.Project

class BundleRepository(val project: Project, val sync: DeploySynchronizer) {

    fun ask(): BundleResponse {
        return BundleResponse.fromJson(sync.get(sync.bundlesUrl))
    }

}