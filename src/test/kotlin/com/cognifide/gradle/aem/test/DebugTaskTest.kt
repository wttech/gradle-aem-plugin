package com.cognifide.gradle.aem.test

import org.junit.Test

class DebugTaskTest : AemTest() {

    @Test
    fun shouldGenerateValidJsonFileForDefaults() {
        buildTask("debug/defaults", ":aemDebug", {
            assertFileExists("Debug output file does not exist.", "build/aem/aemDebug/debug.json")

            // TODO compare json with template, ignore dynamic values like 'buildCount' etc
        })
    }

    @Test
    fun shouldGenerateValidJsonFileForOverrides() {
        buildTask("debug/overrides", ":aemDebug", {
            assertFileExists("Debug output file does not exist.", "build/aem/aemDebug/debug.json")

            // TODO compare json with template, ignore dynamic values like 'buildCount' etc
        })
    }

}