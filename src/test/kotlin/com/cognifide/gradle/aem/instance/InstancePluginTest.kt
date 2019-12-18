package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.utils.using
import com.cognifide.gradle.aem.instance.rcp.InstanceRcp
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceSetup
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class InstancePluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply("com.cognifide.aem.instance")

        extensions.getByName("aem")
        tasks.getByName(InstanceUp.NAME)
        tasks.getByName(InstanceSetup.NAME)
        tasks.getByName(InstanceSatisfy.NAME)
        tasks.getByName(InstanceRcp.NAME)
    }

}