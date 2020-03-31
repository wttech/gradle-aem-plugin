package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.BasePlugin
import javax.inject.Inject

/**
 * Provides 'aem' extension to build script on which all other build logic is based.
 */
class CommonPlugin @Inject constructor(private val componentFactory: SoftwareComponentFactory) : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        AemPlugin.apply { once { logger.info("Using: $NAME_WITH_VERSION") } }

        plugins.apply(BasePlugin::class.java)
        extensions.add(AemExtension.NAME, AemExtension(this))
        components.add(componentFactory.adhoc(COMPONENT))
    }

    companion object {
        const val ID = "com.cognifide.aem.common"

        const val COMPONENT = "aem"
    }
}
