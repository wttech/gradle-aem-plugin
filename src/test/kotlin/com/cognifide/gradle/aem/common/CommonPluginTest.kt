package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.common.utils.using
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommonPluginTest {

    @Test
    fun `plugin registers extension`() = ProjectBuilder.builder().build().using {
        plugins.apply(CommonPlugin.ID)

        extensions.getByName(AemExtension.NAME)
        extensions.getByType(AemExtension::class.java).apply {
            val instances = instanceManager.defined.get()

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
