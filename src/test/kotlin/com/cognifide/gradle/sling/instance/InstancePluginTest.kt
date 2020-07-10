package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.instance.tasks.InstanceRcp
import com.cognifide.gradle.sling.instance.tasks.InstanceSetup
import com.cognifide.gradle.sling.pkg.PackagePlugin
import com.cognifide.gradle.sling.test.SlingTest
import org.gradle.api.internal.plugins.PluginApplicationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InstancePluginTest : SlingTest() {

    @Test
    fun `plugin registers extension and tasks`() = usingProject {
        plugins.apply(InstancePlugin.ID)

        extensions.getByName(SlingExtension.NAME)
        tasks.getByName(InstanceSetup.NAME)
        tasks.getByName(InstanceRcp.NAME)
    }

    @Test
    fun `should not be applied after package plugin`() = usingProject {
        plugins.apply(PackagePlugin.ID)
        assertThrows<PluginApplicationException> {
            plugins.apply(InstancePlugin.ID)
        }
    }
}
