package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.utils.using
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommonPluginTest {

    @Test
    fun `plugin registers extension`() = using(ProjectBuilder.builder().build()) {
        plugins.apply("com.cognifide.aem.common")

        extensions.getByName("aem")
        extensions.getByType(AemExtension::class.java).apply {
            val instances = instanceOptions.definedList

            assertEquals(2, instances.size)

            instances[0].apply {
                assertEquals("local-author", name)
                assertTrue(author)
            }
            instances[1].apply {
                assertEquals("local-publish", name)
                assertTrue(publish)
            }

            assertEquals("/apps/test/install", packageOptions.installPath.get())
        }

        assertTrue(
                tasks.none { it.group == AemTask.GROUP },
                "Common plugin should not provide any tasks which belongs to group AEM."
        )
    }

}