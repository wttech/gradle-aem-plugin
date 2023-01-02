package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.testing.Test

/**
 * Provides 'aem' extension to build script on which all other build logic is based.
 */
class CommonPlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        AemPlugin.apply { once { logger.info("Using: $NAME_WITH_VERSION") } }

        plugins.apply(BasePlugin::class.java)

        val aem = AemExtension(this)
        extensions.add(AemExtension.NAME, aem)

        aem.common.javaSupport.version.convention("11") // valid for instance creation and bundle compilation
        tasks.withType(Test::class.java) { it.javaLauncher.convention(aem.common.javaSupport.launcher) } // use same Java to launch AEM and to run tests on it
    }

    companion object {
        const val ID = "com.cognifide.aem.common"
    }
}
