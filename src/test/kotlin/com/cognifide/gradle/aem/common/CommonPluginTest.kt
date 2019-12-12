package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.tasks.Debug
import com.cognifide.gradle.aem.common.utils.using
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommonPluginTest {

    @Test
    fun `plugin registers extension`() = using(ProjectBuilder.builder().build()) {
        plugins.apply("com.cognifide.aem.common")
        extensions.getByName("aem")

        assertTrue(
                tasks.filter { Debug.NAME != it.name }.none { it.group == AemTask.GROUP },
                "Common plugin should not provide any tasks other than 'debug' task."
        )
    }

}