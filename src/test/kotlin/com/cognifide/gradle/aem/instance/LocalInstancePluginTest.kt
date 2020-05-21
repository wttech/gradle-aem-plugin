package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.instance.tasks.InstanceRcp
import com.cognifide.gradle.aem.instance.tasks.InstanceDown
import com.cognifide.gradle.aem.instance.tasks.InstanceSetup
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.test.AemTest
import org.gradle.api.internal.plugins.PluginApplicationException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocalInstancePluginTest : AemTest() {

    @Test
    fun `plugin registers extension and tasks`() = usingProject {
        plugins.apply(LocalInstancePlugin.ID)
        assertTrue(plugins.hasPlugin(InstancePlugin.ID))

        extensions.getByName(AemExtension.NAME)
        tasks.getByName(InstanceUp.NAME)
        tasks.getByName(InstanceDown.NAME)

        tasks.getByName(InstanceSetup.NAME)
        tasks.getByName(InstanceRcp.NAME)
    }

    @Test
    fun `should not be applied after package plugin`() = usingProject {
        plugins.apply(PackagePlugin.ID)
        assertThrows<PluginApplicationException> {
            plugins.apply(LocalInstancePlugin.ID)
        }
    }
}
