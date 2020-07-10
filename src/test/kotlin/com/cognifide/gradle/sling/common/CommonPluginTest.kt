package com.cognifide.gradle.sling.common

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.SlingTask
import com.cognifide.gradle.sling.test.SlingTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommonPluginTest : SlingTest() {

    @Test
    fun `plugin registers extension`() = usingProject {
        plugins.apply(CommonPlugin.ID)

        extensions.getByName(SlingExtension.NAME)
        extensions.getByType(SlingExtension::class.java).apply {
            val instances = instanceManager.defined.get()

            assertEquals(2, instances.size)

            instances[0].apply {
                assertEquals("local-author", name)
                assertTrue(author)
                assertNotNull(json)
            }
            instances[1].apply {
                assertEquals("local-publish", name)
                assertTrue(publish)
                assertNotNull(json)
            }

            assertEquals("/apps/test/install", packageOptions.installPath.get())
        }

        assertTrue(
                tasks.none { it.group == SlingTask.GROUP },
                "Common plugin should not provide any tasks which belongs to group Sling."
        )
    }
}
