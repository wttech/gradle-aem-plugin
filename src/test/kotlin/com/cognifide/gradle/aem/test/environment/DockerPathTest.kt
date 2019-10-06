package com.cognifide.gradle.aem.test.environment

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.base.DockerPath
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DockerPathTest {

    @Test
    fun shouldImitateProperly() {
        val dockerPath = dockerPath()

        assertEquals(
                "/c/Users/krystian.panek/Projects/gradle-aem-multi/aem/.environment/distributions/mod_dispatcher.so",
                dockerPath.imitateCygpath("C:\\Users\\krystian.panek\\Projects\\gradle-aem-multi\\aem\\.environment\\distributions\\mod_dispatcher.so")
        )

        assertEquals(
                "/c/Users/krystian.panek/Projects/gradle-aem-multi/aem/.environment/distributions/mod_dispatcher.so",
                dockerPath.imitateCygpath("C:/Users/krystian.panek/Projects/gradle-aem-multi/aem/.environment/distributions/mod_dispatcher.so")
        )
    }

    private fun dockerPath() = DockerPath(AemExtension(ProjectBuilder.builder().build()).environment)
}
