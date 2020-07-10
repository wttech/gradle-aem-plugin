package com.cognifide.gradle.sling.pkg

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.pkg.tasks.*
import com.cognifide.gradle.sling.test.SlingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PackagePluginTest : SlingTest() {

    @Test
    fun `plugin registers extension and tasks`() = usingProject {
        plugins.apply(PackagePlugin.ID)

        extensions.getByName(SlingExtension.NAME)

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
