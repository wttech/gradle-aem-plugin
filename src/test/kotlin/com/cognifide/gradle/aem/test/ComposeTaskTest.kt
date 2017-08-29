package com.cognifide.gradle.aem.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class ComposeTaskTest : BuildTest() {

    @Test
    fun shouldComposePackageWithBundleAndContent() {
        buildScript("compose/bundle-and-content", { runner, projectDir ->
            val build = runner.withArguments("aemCompose", "-i", "-S").build()
            assertTaskOutcome(build, ":aemCompose")

            val pkg = assertPackage(projectDir, "build/distributions/example-1.0.0-SNAPSHOT.zip")

            assertPackageVaultFiles(pkg)
            assertPackageFile(pkg, "jcr_root/apps/example/.content.xml")
            assertPackageFile(pkg, "jcr_root/apps/example/install/example-1.0.0-SNAPSHOT.jar")
        })
    }

    @Test
    fun shouldComposePackageAssemblyAndSingles() {
        buildScript("compose/assembly", { runner, projectDir ->
            val build = runner.withArguments("aemCompose", "-i", "-S").build()
            assertTaskOutcomes(build, ":aemCompose", TaskOutcome.SUCCESS)

            val assemblyPkg = assertPackage(projectDir, "build/distributions/example-1.0.0-SNAPSHOT.zip")

            assertPackageFile(assemblyPkg, "jcr_root/apps/example/core/.content.xml")
            assertPackageFile(assemblyPkg, "jcr_root/apps/example/core/install/core-1.0.0-SNAPSHOT.jar")

            assertPackageFile(assemblyPkg, "jcr_root/apps/example/common/.content.xml")
            assertPackageFile(assemblyPkg, "jcr_root/apps/example/common/install/common-1.0.0-SNAPSHOT.jar")
            assertPackageFile(assemblyPkg, "jcr_root/apps/example/common/install/kotlin-osgi-bundle-1.1.4.jar")

            assertPackageFile(assemblyPkg, "jcr_root/etc/designs/example/.content.xml")

            val corePkg = assertPackage(projectDir, "core/build/distributions/example-core-1.0.0-SNAPSHOT.zip")
            assertPackageFile(corePkg, "jcr_root/apps/example/core/.content.xml")
            assertPackageFile(corePkg, "jcr_root/apps/example/core/install/core-1.0.0-SNAPSHOT.jar")

            val commonPkg = assertPackage(projectDir, "common/build/distributions/example-common-1.0.0-SNAPSHOT.zip")
            assertPackageFile(commonPkg, "jcr_root/apps/example/common/.content.xml")
            assertPackageFile(commonPkg, "jcr_root/apps/example/common/install/common-1.0.0-SNAPSHOT.jar")
            assertPackageFile(commonPkg, "jcr_root/apps/example/common/install/kotlin-osgi-bundle-1.1.4.jar")

            val designPkg = assertPackage(projectDir, "design/build/distributions/example-design-1.0.0-SNAPSHOT.zip")
            assertPackageFile(designPkg, "jcr_root/etc/designs/example/.content.xml")
        })
    }

}