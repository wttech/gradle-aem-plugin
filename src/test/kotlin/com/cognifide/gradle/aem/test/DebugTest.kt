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
        buildTask("debug/$buildName", ":${Debug.NAME}") {
            assertJsonCustomized(
                    readFile("debug/$buildName/debug.json"),
                    readFile(file("build/aem/${Debug.NAME}/debug.json")),
                    JSON_CUSTOMIZATIONS
            )
        }
    }

    companion object {
        val JSON_CUSTOMIZATIONS = ValueMatcher.customizationsOf(mapOf(
                "buildInfo" to AnyValueMatcher(),
                "projectInfo.dir" to PathValueMatcher(),
                "aem.configDir" to PathValueMatcher(),
                "aem.configCommonDir" to PathValueMatcher(),
                "aem.localInstanceManager.rootDir" to PathValueMatcher(),
                "aem.localInstanceManager.overridesDir" to PathValueMatcher(),
                "aem.localInstanceManager.quickstart.downloadDir" to PathValueMatcher(),
                "aem.localInstanceManager.backup.remoteDir" to PathValueMatcher(),
                "aem.localInstanceManager.backup.localDir" to PathValueMatcher(),
                "aem.localInstanceManager.install.downloadDir" to PathValueMatcher(),
                "aem.packageOptions.contentDir" to PathValueMatcher(),
                "aem.packageOptions.metaCommonDir" to PathValueMatcher(),
                "aem.tasks.bundles[*].bndPath" to PathValueMatcher(),
                "aem.environment.dockerComposeFile" to PathValueMatcher(),
                "aem.environment.dockerComposeSourceFile" to PathValueMatcher(),
                "aem.environment.dockerPath.cygpathPath" to PathValueMatcher(),
                "aem.environment.dockerRootPath" to PathValueMatcher(),
                "aem.environment.dockerConfigPath" to PathValueMatcher(),
                "aem.environment.directories.caches[*]" to PathValueMatcher(),
                "aem.environment.directories.regulars[*]" to PathValueMatcher(),
                "aem.environment.rootDir" to PathValueMatcher(),
                "aem.environment.configDir" to PathValueMatcher(),
                "aem.environment.httpdConfDir" to PathValueMatcher()
        ))
    }

}
