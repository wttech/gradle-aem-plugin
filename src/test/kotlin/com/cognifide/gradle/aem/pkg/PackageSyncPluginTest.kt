package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.pkg.tasks.PackageSync
import com.cognifide.gradle.aem.pkg.tasks.PackageVlt
import com.cognifide.gradle.aem.test.AemTest
import org.junit.jupiter.api.Test

class PackageSyncPluginTest : AemTest() {

    @Test
    fun `plugin registers extension and tasks`() = usingProject {
        plugins.apply(PackageSyncPlugin.ID)

        extensions.getByName(AemExtension.NAME)
        tasks.getByName(PackageSync.NAME)
        tasks.getByName(PackageVlt.NAME)
    }
}
