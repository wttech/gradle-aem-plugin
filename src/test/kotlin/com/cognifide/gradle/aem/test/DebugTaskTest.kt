package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.test.AemAssert.assertJsonCustomized
import com.cognifide.gradle.aem.test.json.AnyValueMatcher
import com.cognifide.gradle.aem.test.json.PathValueMatcher
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.Customization

class DebugTaskTest : AemTest() {

    companion object {
        val JSON_CUSTOMIZATIONS by lazy {
            mutableListOf<Customization>().apply {
                add(Customization("buildInfo", AnyValueMatcher()))
                add(Customization("projectInfo.dir", PathValueMatcher()))
            }
        }
    }

    @Test
    fun shouldGenerateValidJsonFileForMinimal() {
        buildDebugJsonFile("minimal")
    }

    @Test
    fun shouldGenerateValidJsonFileForAdditional() {
        buildDebugJsonFile("additional")
    }

    private fun buildDebugJsonFile(buildName: String) {
        build("debug/$buildName", {
            withArguments(":aemDebug", "-S", "-i", "--offline")
        }, {
            assertJsonCustomized(
                    readFile("debug/$buildName/debug.json"),
                    readFile(file("build/aem/aemDebug/debug.json")),
                    JSON_CUSTOMIZATIONS
            )
        })
    }

}