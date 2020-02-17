package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.using
import com.cognifide.gradle.aem.instance.rcp.InstanceRcp
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import com.cognifide.gradle.aem.instance.tasks.InstanceDown
import com.cognifide.gradle.aem.instance.tasks.InstanceSetup
import com.cognifide.gradle.aem.instance.tasks.InstanceUp
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class LocalInstancePluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply(LocalInstancePlugin.ID)

        extensions.getByName(AemExtension.NAME)
        tasks.getByName(InstanceUp.NAME)
        tasks.getByName(InstanceDown.NAME)

        tasks.getByName(InstanceSetup.NAME)
        tasks.getByName(InstanceSatisfy.NAME)
        tasks.getByName(InstanceRcp.NAME)
    }

}