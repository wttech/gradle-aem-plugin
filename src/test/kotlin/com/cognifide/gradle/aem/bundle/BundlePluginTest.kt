package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.bundle.tasks.BundleInstall
import com.cognifide.gradle.aem.bundle.tasks.BundleUninstall
import com.cognifide.gradle.aem.common.utils.using
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BundlePluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply(BundlePlugin.ID)

        extensions.getByName(AemExtension.NAME)

        tasks.named(BundleCompose.NAME, BundleCompose::class.java).get().apply {
            assertEquals("test.jar", composedFile.name)
        }

        tasks.getByName(BundleInstall.NAME)
        tasks.getByName(BundleUninstall.NAME)
    }
}