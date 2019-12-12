package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.common.utils.using
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.aem.pkg.tasks.PackageDeploy
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class PackagePluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply("com.cognifide.aem.package")

        extensions.getByName("aem")
        tasks.getByName(PackageCompose.NAME)
        tasks.getByName(PackageDeploy.NAME)
    }

}