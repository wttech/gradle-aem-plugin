package com.cognifide.gradle.aem.test

import org.junit.Test

class DebugTaskTest : AemTest() {

    companion object {
        val IGNORED_FIELDS = listOf(
                "projectInfo.dir", // TODO apply rule for checking paths
                "packageProperties.buildCount", // TODO apply rule for checking type
                "packageProperties.created", // TODO apply rule for checking type
                "packageProperties.config.contentPath", // TODO apply rule for checking paths
                "packageProperties.config.vaultFilesPath", // TODO apply rule for checking paths
                "packageProperties.config.instanceFilesPath", // TODO apply rule for checking paths
                "packageProperties.config.checkoutFilterPath", // TODO apply rule for checking paths
                "packageProperties.config.buildDate" // TODO apply rule for checking type

        )
    }

    @Test
    fun shouldGenerateValidJsonFileForMinimal() {
        build("debug/minimal", {
            it.withArguments(":aemDebug", "-S", "-i", "--offline")
        }, {
            assertJson(
                    readFile("debug/minimal/debug.json"),
                    readFile(file("build/aem/aemDebug/debug.json")),
                    IGNORED_FIELDS

            )
        })
    }

    @Test
    fun shouldGenerateValidJsonFileForAdditional() {
        build("debug/additional", {
            it.withArguments(":aemDebug", "-S", "-i", "--offline")
        }, {
            assertJson(
                    readFile("debug/additional/debug.json"),
                    readFile(file("build/aem/aemDebug/debug.json")),
                    IGNORED_FIELDS
            )
        })
    }

}