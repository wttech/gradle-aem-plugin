package com.cognifide.gradle.aem.launcher

import org.buildobjects.process.ProcBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class LauncherTest {

    @Test
    fun shouldRunProperly() = makeTestProject("tasks", null) {
        launch("tasks")
        assertEquals(
            File("../gradle/wrapper/gradle-wrapper.properties").readAsProperties()["distributionUrl"],
            resolve("gradle/wrapper/gradle-wrapper.properties").readAsProperties()["distributionUrl"]
        )
    }

    @Test
    fun shouldDisplayInstanceStatus() = makeTestProject("instance-status", "6.5.10") {
        launch("instanceStatus", "-Pinstance.local-author.httpUrl=http://localhost:8502") // some unavailable instance
    }

    @Test
    fun shouldGenerateCloudEnvProperly() = makeTestProject("cloud-env", "cloud") {
        launch() // no environmentResolve as sdk zip is required for it to work
        assertTrue(
            File("build/functionalTest/cloud-env/env/src/environment/httpd/conf.d/variables/default.vars").readText()
                .contains("COMMERCE_ENDPOINT")
        )
    }

    @Test
    fun shouldGenerateOnPremEnvProperly() = makeTestProject("on-prem-env", "6.5.10") {
        launch("env:environmentResolve")
        assertTrue(
            File("build/functionalTest/on-prem-env/env/src/environment/httpd/conf.d/variables/default.vars").readText()
                .contains("AUTHOR_DEFAULT_HOSTNAME")
        )
    }

    @Test
    fun shouldNotGenerateEnvironmentWithoutArchetypePropertiesFile() {
        makeTestProject("no-env", null) {
            assertFalse(File("build/functionalTest/no-env/env/src").exists())
        }
    }

    @Test
    fun shouldRespectGradleWrapperProperties() {
        makeTestProject("custom-gradle-version", null) {
            launch("--gradle-version=7.4.2", "--distribution-type=all")
            assertEquals(
                "https://services.gradle.org/distributions/gradle-7.4.2-all.zip",
                resolve("gradle/wrapper/gradle-wrapper.properties").readAsProperties()["distributionUrl"]
            )
        }
    }

    @Test
    fun shouldRespectGradleWrapperDistribution() {
        makeTestProject("custom-gradle-distribution", null) {
            launch("--gradle-distribution-url=http://my-custom-gradle.distribution/dist.zip")
            assertEquals(
                "http://my-custom-gradle.distribution/dist.zip",
                resolve("gradle/wrapper/gradle-wrapper.properties").readAsProperties()["distributionUrl"]
            )
        }
    }

    private fun makeTestProject(name: String, aemVersion: String?, callback: File.() -> Unit) {
        File("build/functionalTest/$name").apply {
            deleteRecursively()
            mkdirs()
            if (aemVersion != null) {
                createArchetypePropertiesFile(aemVersion)
            }
            callback()
        }
    }

    private fun File.createArchetypePropertiesFile(aemVersion: String) {
        resolve("archetype.properties").let { f ->
            f.outputStream().use { out ->
                Properties().apply {
                    put("aemVersion", aemVersion)
                    store(out, null)
                }
            }
        }
    }

    private fun File.readAsProperties(): Properties {
        return Properties().apply {
            load(this@readAsProperties.inputStream())
        }
    }

    @Suppress("MagicNumber")
    private fun File.launch(vararg args: String) {
        ProcBuilder("java", "-jar", File("build/libs/gap.jar").absolutePath, *args).apply {
            withWorkingDirectory(this@launch)
            withOutputStream(System.out)
            withErrorStream(System.err)
            withTimeoutMillis(TimeUnit.SECONDS.toMillis(120))
            run()
        }
    }
}
