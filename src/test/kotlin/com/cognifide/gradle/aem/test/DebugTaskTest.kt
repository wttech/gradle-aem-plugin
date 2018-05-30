package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.test.AemAssert.assertJsonCustomized
import com.cognifide.gradle.aem.test.json.AnyValueMatcher
import com.cognifide.gradle.aem.test.json.DateValueMatcher
import com.cognifide.gradle.aem.test.json.PathValueMatcher
import org.junit.Test
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.RegularExpressionValueMatcher

class DebugTaskTest : AemTest() {

    companion object {
        val JSON_CUSTOMIZATIONS by lazy {
            mutableListOf<Customization>().apply {
                add(Customization("buildInfo", AnyValueMatcher()))
                add(Customization("projectInfo.dir", PathValueMatcher()))
                add(Customization("packageProperties.buildCount", RegularExpressionValueMatcher("\\d{14}")))
                add(Customization("packageProperties.created", DateValueMatcher()))
                add(Customization("packageProperties.config.packageBuildDate", RegularExpressionValueMatcher("\\d{13}")))
                add(Customization("packageProperties.config.contentPath", PathValueMatcher()))
                add(Customization("packageProperties.config.vaultFilesPath", PathValueMatcher()))
                add(Customization("packageProperties.config.createFilesPath", PathValueMatcher()))
                add(Customization("packageProperties.config.createPath", PathValueMatcher()))
                add(Customization("packageProperties.config.bundleBndPath", PathValueMatcher()))
            }
        }
    }

    @Test
    fun shouldGenerateValidJsonFileForMinimal() {
        build("debug/minimal", {
            withArguments(":aemDebug", "-S", "-i", "--offline")
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
            withArguments(":aemDebug", "-S", "-i", "--offline")
        }, {
            assertJsonCustomized(
                    readFile("debug/additional/debug.json"),
                    readFile(file("build/aem/aemDebug/debug.json")),
                    JSON_CUSTOMIZATIONS
            )
        })
    }

}