package com.cognifide.gradle.aem.test

import org.junit.Test
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.ValueMatcher

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
            }
        }
    }

    class PathValueMatcher(val prefix: String) : ValueMatcher<String> {
        override fun equal(p1: String?, p2: String?): Boolean {
            if (p1 == p2) {
                return true
            }

            return if (p1 != null && p2 != null) {
                normalizePath(p1) == normalizePath(p2)
            } else {
                false
            }
        }

        private fun normalizePath(path: String): String {
            return path.replace("\\", "/").substringAfter(prefix)
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