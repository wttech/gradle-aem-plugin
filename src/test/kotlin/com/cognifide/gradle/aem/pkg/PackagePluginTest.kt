package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.using
import com.cognifide.gradle.aem.pkg.tasks.*
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PackagePluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply(PackagePlugin.ID)

        extensions.getByName(AemExtension.NAME)

        tasks.getByName(PackageActivate.NAME)

        tasks.named(PackageCompose.NAME, PackageCompose::class.java).get().apply {
            assertEquals("test.zip", composedFile.name)
        }

        tasks.getByName(PackageDelete.NAME)
        tasks.getByName(PackageDeploy.NAME)
        tasks.getByName(PackageInstall.NAME)
        tasks.getByName(PackagePrepare.NAME)
        tasks.getByName(PackagePurge.NAME)
        tasks.getByName(PackageUninstall.NAME)
        tasks.getByName(PackageUpload.NAME)
    }
}