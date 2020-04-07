package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

/**
 * Provides 'aem' extension to build script on which all other build logic is based.
 */
class CommonPlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        AemPlugin.apply { once { logger.info("Using: $NAME_WITH_VERSION") } }

        plugins.apply(BasePlugin::class.java)
        extensions.add(AemExtension.NAME, AemExtension(this))
        repositories.jcenter() // needed by CLI apps
    }

    companion object {
        const val ID = "com.cognifide.aem.common"
    }
}
