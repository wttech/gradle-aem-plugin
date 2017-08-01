package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.deploy.DeploySynchronizer
import org.apache.commons.httpclient.params.HttpConnectionParams
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class InstanceState(val project: Project, val instance: Instance, val timeout: Int) {

    val logger: Logger = project.logger

    val config = AemConfig.of(project)

    val sync = DeploySynchronizer(instance, config)

    private var syncParametrizer: (HttpConnectionParams) -> Unit = { params ->
        params.connectionTimeout = timeout
        params.soTimeout = timeout
    }

    val bundleState by lazy {
        try {
            BundleState.fromJson(sync.get(sync.bundlesUrl, syncParametrizer))
        } catch (e: Exception) {
            logger.debug("Instance bundles asking error", e)
            BundleState.unknown(e)
        }
    }

    val stable: Boolean
        get() = bundleState.stable

}