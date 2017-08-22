package com.cognifide.gradle.aem.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DebugTaskTest : BuildTest() {

    @Test
    fun shouldGenerateValidJsonFile() {
        buildScript("debug", { configurer, projectDir ->
            val build = configurer.withArguments("aemDebug", "-i", "-S").build()

            assertEquals(TaskOutcome.SUCCESS, build.task(":aemDebug").outcome)
            assertTrue("Debug output file does not exist.", File(projectDir, "build/aem/aemDebug/debug.json").exists())
        })
    }

}