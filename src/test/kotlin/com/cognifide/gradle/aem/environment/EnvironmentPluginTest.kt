package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.utils.using
import com.cognifide.gradle.aem.environment.tasks.EnvironmentAwait
import com.cognifide.gradle.aem.environment.tasks.EnvironmentDev
import com.cognifide.gradle.aem.environment.tasks.EnvironmentDown
import com.cognifide.gradle.aem.environment.tasks.EnvironmentUp
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class EnvironmentPluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply("com.cognifide.aem.environment")

        extensions.getByName("aem")
        tasks.getByName(EnvironmentUp.NAME)
        tasks.getByName(EnvironmentAwait.NAME)
        tasks.getByName(EnvironmentDev.NAME)
        tasks.getByName(EnvironmentDown.NAME)
    }

}