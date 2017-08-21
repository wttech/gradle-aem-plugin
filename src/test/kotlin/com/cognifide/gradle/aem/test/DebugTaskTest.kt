package com.cognifide.gradle.aem.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DebugTaskTest : RunnerTest() {

    @Test
    fun shouldGenerateValidJsonFile() {
        buildScript("debug", { configurer, projectDir ->
            val buildResult = configurer.withArguments("aemDebug").build()

            assertEquals(TaskOutcome.SUCCESS, buildResult.task(":aemDebug").outcome)
            assertTrue("Debug output file does not exist.", File(projectDir, "build/aem/aemDebug/debug.json").exists())
        })
    }

}