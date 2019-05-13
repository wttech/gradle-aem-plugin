package com.cognifide.gradle.aem.test.integration

import com.cognifide.gradle.aem.config.tasks.Debug
import com.cognifide.gradle.aem.test.AemAssert.assertJsonCustomized
import com.cognifide.gradle.aem.test.json.AnyValueMatcher
import com.cognifide.gradle.aem.test.json.PathValueMatcher
import com.cognifide.gradle.aem.test.json.ValueMatcher
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled // TODO move 'config' to 'aem(extension')
class DebugTest : AemTest() {

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
            withArguments(":${Debug.NAME}", "-S", "-i", "-Pdebug.packageDeployed=false")
        }, {
            assertJsonCustomized(
                    readFile("debug/$buildName/debug.json"),
                    readFile(file("build/aem/${Debug.NAME}/debug.json")),
                    JSON_CUSTOMIZATIONS
            )
        })
    }

    companion object {
        val JSON_CUSTOMIZATIONS = ValueMatcher.customizationsOf(mapOf(
                "buildInfo" to AnyValueMatcher(),
                "projectInfo.dir" to PathValueMatcher(),
                "baseConfig.localInstanceOptions.root" to PathValueMatcher(),
                "baseConfig.localInstanceOptions.overridesPath" to PathValueMatcher(),
                "baseConfig.packageRoot" to PathValueMatcher(),
                "baseConfig.packageMetaCommonRoot" to PathValueMatcher(),
                "baseConfig.groovyScriptRoot" to PathValueMatcher(),
                "bundleConfig[*].bndPath" to PathValueMatcher(),
                "baseConfig.environmentOptions.dispatcherModuleFile" to PathValueMatcher(),
                "baseConfig.environmentOptions.dockerComposeFile" to PathValueMatcher(),
                "baseConfig.environmentOptions.dockerComposeSourceFile" to PathValueMatcher(),
                "baseConfig.environmentOptions.root" to PathValueMatcher(),
                "baseConfig.environmentOptions.httpdConfDir" to PathValueMatcher()
        ))
    }

}