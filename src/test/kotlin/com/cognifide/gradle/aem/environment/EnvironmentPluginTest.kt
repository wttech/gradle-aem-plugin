package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.utils.using
import com.cognifide.gradle.aem.environment.tasks.*
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class EnvironmentPluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply("com.cognifide.aem.environment")

        extensions.getByName("aem")
        tasks.getByName(EnvironmentAwait.NAME)
        tasks.getByName(EnvironmentDestroy.NAME)
        tasks.getByName(EnvironmentDev.NAME)
        tasks.getByName(EnvironmentDown.NAME)
        tasks.getByName(EnvironmentHosts.NAME)
        tasks.getByName(EnvironmentReload.NAME)
        tasks.getByName(EnvironmentResetup.NAME)
        tasks.getByName(EnvironmentResolve.NAME)
        tasks.getByName(EnvironmentRestart.NAME)
        tasks.getByName(EnvironmentUp.NAME)
    }

}