package com.cognifide.gradle.aem.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ComposeTaskTest : BuildTest() {

    @Test
    fun shouldComposePackageWithBundleAndContent() {
        buildScript("compose/bundle-and-content", { runner, projectDir ->
            val build = runner.withArguments(":aemCompose", "-i", "-S").build()
            assertEquals(TaskOutcome.SUCCESS, build.task(":aemCompose").outcome)

            val pkg = File(projectDir, "build/distributions/example-1.0.0-SNAPSHOT.zip")
            assertTrue("Composed CRX package does not exist.", pkg.exists())

            assertPackageVaultFiles(pkg)
            assertPackageFile(pkg, "jcr_root/apps/example/.content.xml")
            assertPackageFile(pkg, "jcr_root/apps/example/install/example-1.0.0-SNAPSHOT.jar")
        })
    }

}