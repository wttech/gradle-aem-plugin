package com.cognifide.gradle.sling.pkg

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.pkg.tasks.PackageSync
import com.cognifide.gradle.sling.pkg.tasks.PackageVlt
import com.cognifide.gradle.sling.test.SlingTest
import org.junit.jupiter.api.Test

class PackageSyncPluginTest : SlingTest() {

    @Test
    fun `plugin registers extension and tasks`() = usingProject {
        plugins.apply(PackageSyncPlugin.ID)

        extensions.getByName(SlingExtension.NAME)
        tasks.getByName(PackageSync.NAME)
        tasks.getByName(PackageVlt.NAME)
    }
}
