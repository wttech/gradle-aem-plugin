package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.using
import com.cognifide.gradle.aem.instance.tasks.InstanceRcp
import com.cognifide.gradle.aem.instance.tasks.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceSetup
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class InstancePluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply(InstancePlugin.ID)

        extensions.getByName(AemExtension.NAME)
        tasks.getByName(InstanceSetup.NAME)
        tasks.getByName(InstanceSatisfy.NAME)
        tasks.getByName(InstanceRcp.NAME)
    }

}