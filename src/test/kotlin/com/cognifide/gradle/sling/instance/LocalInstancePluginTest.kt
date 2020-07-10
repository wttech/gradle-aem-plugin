package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.instance.tasks.InstanceRcp
import com.cognifide.gradle.sling.instance.tasks.InstanceDown
import com.cognifide.gradle.sling.instance.tasks.InstanceSetup
import com.cognifide.gradle.sling.instance.tasks.InstanceUp
import com.cognifide.gradle.sling.pkg.PackagePlugin
import com.cognifide.gradle.sling.test.SlingTest
import org.gradle.api.internal.plugins.PluginApplicationException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocalInstancePluginTest : SlingTest() {

    @Test
    fun `plugin registers extension and tasks`() = usingProject {
        plugins.apply(LocalInstancePlugin.ID)
        assertTrue(plugins.hasPlugin(InstancePlugin.ID))

        extensions.getByName(SlingExtension.NAME)
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
