package com.cognifide.gradle.aem.instance.local

import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

class ResetupTest : AemBuildTest() {

    @EnabledIfSystemProperty(named = "localInstance.jarUrl", matches = ".+")
    @Test
    fun `should re-setup local aem author and publish instances`() {
        val projectDir = prepareProject("local-instance-resetup") {
            gradleProperties("""
                fileTransfer.user=${System.getProperty("fileTransfer.user")}
                fileTransfer.password=${System.getProperty("fileTransfer.password")}
                fileTransfer.domain=${System.getProperty("fileTransfer.domain")}
                
                localInstance.quickstart.jarUrl=${System.getProperty("localInstance.jarUrl")}
                localInstance.quickstart.licenseUrl=${System.getProperty("localInstance.licenseUrl")}
                
                instance.local-author.httpUrl=http://localhost:9502
                instance.local-author.type=local
                instance.local-author.runModes=local,nosamplecontent
                instance.local-author.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true
                """)

            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance.local")
                }
                """)
        }

        runBuild(projectDir, "instanceResetup", "-Pforce") {
            assertTask(":instanceDown")
            assertTask(":instanceDestroy")
            assertTask(":instanceCreate")
            assertTask(":instanceUp")
            assertTask(":instanceSatisfy")
            assertTask(":instanceProvision")
            assertTask(":instanceSetup")
            assertTask(":instanceResetup")
            assertFileExists(".instance/author")
            assertFileNotExists(".instance/publish")
        }

        runBuild(projectDir, "instanceDestroy", "-Pforce") {
            assertTask(":instanceDestroy")
        }

        runBuild(projectDir, "clean") {
            assertTask(":clean")
        }
    }
}