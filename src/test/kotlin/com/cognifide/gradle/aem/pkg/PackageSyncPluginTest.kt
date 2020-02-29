package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.pkg.tasks.PackageSync
import com.cognifide.gradle.aem.pkg.tasks.PackageVlt
import com.cognifide.gradle.common.utils.using
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class PackageSyncPluginTest {

    @Test
    fun `plugin registers extension and tasks`() = ProjectBuilder.builder().build().using {
        plugins.apply("com.cognifide.aem.package.sync")

        extensions.getByName("aem")
        tasks.getByName(PackageSync.NAME)
        tasks.getByName(PackageVlt.NAME)
    }
}
