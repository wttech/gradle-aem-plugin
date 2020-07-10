package com.cognifide.gradle.sling.common

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.SlingPlugin
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

/**
 * Provides 'sling' extension to build script on which all other build logic is based.
 */
class CommonPlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        SlingPlugin.apply { once { logger.info("Using: $NAME_WITH_VERSION") } }

        plugins.apply(BasePlugin::class.java)
        extensions.add(SlingExtension.NAME, SlingExtension(this))
    }

    companion object {
        const val ID = "com.cognifide.sling.common"
    }
}
