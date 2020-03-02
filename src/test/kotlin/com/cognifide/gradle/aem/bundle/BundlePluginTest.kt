package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.bundle.tasks.BundleInstall
import com.cognifide.gradle.aem.bundle.tasks.BundleUninstall
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.test.AemTest
import org.gradle.api.internal.plugins.PluginApplicationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BundlePluginTest : AemTest() {

    @Test
    fun `should register extension and tasks`() = usingProject {
        plugins.apply(BundlePlugin.ID)

        extensions.getByName(AemExtension.NAME)

        tasks.named(BundleCompose.NAME, BundleCompose::class.java).get().apply {
            assertEquals("test.jar", composedFile.name)
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
