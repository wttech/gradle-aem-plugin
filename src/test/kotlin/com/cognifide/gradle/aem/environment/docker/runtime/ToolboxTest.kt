package com.cognifide.gradle.aem.environment.docker.runtime

import com.cognifide.gradle.aem.AemExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToolboxTest {

    @Test
    fun shouldImitateCygpathProperly() {
        toolbox().apply {
            assertEquals(
                    "/c/Users/krystian.panek/Projects/gradle-aem-multi/aem/.environment/distributions/mod_dispatcher.so",
                    imitateCygpath("C:\\Users\\krystian.panek\\Projects\\gradle-aem-multi\\aem\\.environment\\distributions\\mod_dispatcher.so")
            )

            assertEquals(
                    "/c/Users/krystian.panek/Projects/gradle-aem-multi/aem/.environment/distributions/mod_dispatcher.so",
                    imitateCygpath("C:/Users/krystian.panek/Projects/gradle-aem-multi/aem/.environment/distributions/mod_dispatcher.so")
            )
        }
    }

    private fun toolbox() = Toolbox(AemExtension(ProjectBuilder.builder().build()))
}
