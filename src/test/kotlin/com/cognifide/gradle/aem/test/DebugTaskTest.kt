package com.cognifide.gradle.aem.test

import org.junit.Test
import org.skyscreamer.jsonassert.Customization

class DebugTaskTest : AemTest() {

    companion object {
        val JSON_CUSTOMIZATIONS by lazy {
            mutableListOf<Customization>().apply {
                add(Customization("projectInfo.dir", { _, _ -> true}))
                add(Customization("packageProperties.buildCount", {_, _ -> true}))
                add(Customization("packageProperties.created", {_, _ -> true}))
                add(Customization("packageProperties.config.contentPath", {_, _ -> true}))
                add(Customization("packageProperties.config.vaultFilesPath", {_, _ -> true}))
                add(Customization("packageProperties.config.instanceFilesPath", {_, _ -> true}))
                add(Customization("packageProperties.config.checkoutFilterPath", {_, _ -> true}))
                add(Customization("packageProperties.config.buildDate", {_, _ -> true}))
                add(Customization("", {_, _ -> true}))
            }
        }
    }

    @Test
    fun shouldGenerateValidJsonFileForMinimal() {
        build("debug/minimal", {
            it.withArguments(":aemDebug", "-S", "-i", "--offline")
        }, {
            assertJsonCustomized(
                    readFile("debug/minimal/debug.json"),
                    readFile(file("build/aem/aemDebug/debug.json")),
                    JSON_CUSTOMIZATIONS

            )
        })
    }

    @Test
    fun shouldGenerateValidJsonFileForAdditional() {
        build("debug/additional", {
            it.withArguments(":aemDebug", "-S", "-i", "--offline")
        }, {
            assertJsonCustomized(
                    readFile("debug/additional/debug.json"),
                    readFile(file("build/aem/aemDebug/debug.json")),
                    JSON_CUSTOMIZATIONS
            )
        })
    }

}