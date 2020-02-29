package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.instance.tasks.InstanceRcp
import com.cognifide.gradle.aem.instance.tasks.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceDown
import com.cognifide.gradle.aem.instance.tasks.InstanceSetup
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import com.cognifide.gradle.common.utils.using
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class LocalInstancePluginTest {

    @Test
    fun `plugin registers extension and tasks`() = ProjectBuilder.builder().build().using {
        plugins.apply(LocalInstancePlugin.ID)

        extensions.getByName(AemExtension.NAME)
        tasks.getByName(InstanceUp.NAME)
        tasks.getByName(InstanceDown.NAME)

        tasks.getByName(InstanceSetup.NAME)
        tasks.getByName(InstanceSatisfy.NAME)
        tasks.getByName(InstanceRcp.NAME)
    }
}
