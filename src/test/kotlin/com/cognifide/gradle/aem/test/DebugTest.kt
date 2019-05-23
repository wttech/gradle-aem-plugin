package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.common.tasks.Debug
import com.cognifide.gradle.aem.test.AemAssert.assertJsonCustomized
import com.cognifide.gradle.aem.test.json.AnyValueMatcher
import com.cognifide.gradle.aem.test.json.PathValueMatcher
import com.cognifide.gradle.aem.test.json.ValueMatcher
import org.junit.jupiter.api.Test

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
                "aem.configDir" to PathValueMatcher(),
                "aem.configCommonDir" to PathValueMatcher(),
                "aem.groovyScriptRootDir" to PathValueMatcher(),
                "aem.localInstanceOptions.rootDir" to PathValueMatcher(),
                "aem.localInstanceOptions.overridesDir" to PathValueMatcher(),
                "aem.packageOptions.rootDir" to PathValueMatcher(),
                "aem.packageOptions.metaCommonRootDir" to PathValueMatcher(),
                "aem.tasks.bundleConfig[*].bndPath" to PathValueMatcher(),
                "aem.environment.dispatcherModuleFile" to PathValueMatcher(),
                "aem.environment.dockerComposeFile" to PathValueMatcher(),
                "aem.environment.dockerComposeSourceFile" to PathValueMatcher(),
                "aem.environment.dockerRootPath" to PathValueMatcher(),
                "aem.environment.dockerConfigPath" to PathValueMatcher(),
                "aem.environment.rootDir" to PathValueMatcher(),
                "aem.environment.configDir" to PathValueMatcher(),
                "aem.environment.httpdConfDir" to PathValueMatcher()
        ))
    }

}