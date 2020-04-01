package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.pkg.tasks.*
import com.cognifide.gradle.aem.test.AemTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PackagePluginTest : AemTest() {

    @Test
    fun `plugin registers extension and tasks`() = usingProject {
        plugins.apply(PackagePlugin.ID)

        extensions.getByName(AemExtension.NAME)

        tasks.getByName(PackageActivate.NAME)

        tasks.named(PackageCompose.NAME, PackageCompose::class.java).get().apply {
            assertEquals("test.zip", archiveFile.get().asFile.name)
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
