package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.test.AemTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommonPluginTest : AemTest() {

    @Test
    fun `plugin registers extension`() = usingProject {
        plugins.apply(CommonPlugin.ID)

        extensions.getByName(AemExtension.NAME)
        extensions.getByType(AemExtension::class.java).apply {
            val instances = instanceManager.defined.get()

            assertEquals(2, instances.size)

            instances[0].apply {
                assertEquals("local-author", name)
                assertTrue(author)
                assertNotNull(this.toString())
            }
            instances[1].apply {
                assertEquals("local-publish", name)
                assertTrue(publish)
                assertNotNull(this.toString())
            }

            assertEquals("/apps/test/install", packageOptions.installPath.get())
        }

        assertTrue(
            tasks.none { it.group == AemTask.GROUP },
            "Common plugin should not provide any tasks which belongs to group AEM."
        )
    }
}
