package com.cognifide.gradle.aem.tooling

import com.cognifide.gradle.aem.common.utils.using
import com.cognifide.gradle.aem.tooling.rcp.Rcp
import com.cognifide.gradle.aem.tooling.sync.Sync
import com.cognifide.gradle.aem.tooling.vlt.Vlt
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class ToolingPluginTest {

    @Test
    fun `plugin registers extension and tasks`() = using(ProjectBuilder.builder().build()) {
        plugins.apply("com.cognifide.aem.tooling")

        extensions.getByName("aem")
        tasks.getByName(Sync.NAME)
        tasks.getByName(Vlt.NAME)
        tasks.getByName(Rcp.NAME)
    }

}