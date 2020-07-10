package com.cognifide.gradle.sling.bundle

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.bundle.tasks.BundleInstall
import com.cognifide.gradle.sling.bundle.tasks.BundleUninstall
import com.cognifide.gradle.sling.pkg.PackagePlugin
import com.cognifide.gradle.sling.test.SlingTest
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BundlePluginTest : SlingTest() {

    @Test
    fun `should register extension and tasks`() = usingProject {
        plugins.apply(BundlePlugin.ID)

        extensions.getByName(SlingExtension.NAME)

        tasks.named(JavaPlugin.JAR_TASK_NAME, Jar::class.java).get().apply {
            assertEquals("test.jar", archiveFile.get().asFile.name)
        }

        tasks.getByName(BundleInstall.NAME)
        tasks.getByName(BundleUninstall.NAME)
    }

    @Test
    fun `should not be applied after package plugin`() = usingProject {
        plugins.apply(PackagePlugin.ID)
        assertThrows<PluginApplicationException> {
            plugins.apply(BundlePlugin.ID)
        }
    }
}
