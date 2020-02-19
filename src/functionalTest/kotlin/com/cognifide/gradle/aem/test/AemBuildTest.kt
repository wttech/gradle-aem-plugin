package com.cognifide.gradle.aem.test

import org.gradle.testkit.runner.GradleRunner
import java.io.File

abstract class AemBuildTest {

    fun prepareProject(path: String, definition: File.() -> Unit) = File("build/functionalTest/$path").apply {
        deleteRecursively()
        mkdirs()
        definition()
    }

    fun File.file(path: String, text: String) {
        resolve(path).apply { parentFile.mkdirs() }.writeText(text.trimIndent())
    }

    fun File.buildSrc(text: String) = file("buildSrc/build.gradle.kts", text)

    fun File.settingsGradle(text: String) = file("settings.gradle.kts", text)

    fun File.buildGradle(text: String) = file("build.gradle.kts", text)

    fun File.gradleProperties(text: String) = file("gradle.properties", text)

    fun runBuild(projectDir: File, vararg arguments: String, asserter: AemBuildResult.() -> Unit) {
        runBuild(projectDir, { withArguments(*arguments, "-i", "-S") }, asserter)
    }

    fun runBuild(projectDir: File, runnerOptions: GradleRunner.() -> Unit, asserter: AemBuildResult.() -> Unit) {
        AemBuildResult(runBuild(projectDir, runnerOptions), projectDir).apply(asserter)
    }

    fun runBuild(projectDir: File, options: GradleRunner.() -> Unit) = GradleRunner.create().run {
        forwardOutput()
        withPluginClasspath()
        withProjectDir(projectDir)
        apply(options)
        build()
    }

    fun File.helloServiceJava(rootPath: String = "") {
        file(rootPath(rootPath, "src/main/java/com/company/example/aem/HelloService.java"), """
            package com.company.example.aem;
            
            import org.osgi.service.component.annotations.Activate;
            import org.osgi.service.component.annotations.Component;
            import org.osgi.service.component.annotations.Deactivate;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            @Component(immediate = true, service = HelloService.class)
            class HelloService {
                               
                private static final Logger LOG = LoggerFactory.getLogger(HelloService.class);
                
                @Activate
                protected void activate() {
                    LOG.info("Hello world!");
                }
                
                @Deactivate
                protected void deactivate() {
                    LOG.info("Good bye world!");
                }
            }
        """)
    }

    fun rootPath(rootPath: String, path: String) = rootPath.takeIf { it.isNotBlank() }?.let { "$it/$path"} ?: path
}