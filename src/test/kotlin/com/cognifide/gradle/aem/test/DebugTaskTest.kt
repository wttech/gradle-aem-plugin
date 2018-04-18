package com.cognifide.gradle.aem.test

import org.junit.Test

class DebugTaskTest : AemTest() {

    @Test
    fun shouldGenerateValidJsonFileForDefaults() {
        buildScript("debug/defaults", "aemDebug", {
            assertFileExists("Debug output file does not exist.", "build/aem/aemDebug/debug.json")
        })
    }

    @Test
    fun shouldGenerateValidJsonFileForOverrides() {
        buildScript("debug/overrides", "aemDebug", {
            assertFileExists("Debug output file does not exist.", "build/aem/aemDebug/debug.json")
        })
    }

}