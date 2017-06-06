package com.cognifide.gradle.aem.jar

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import org.apache.felix.scrplugin.ant.SCRDescriptorTask
import org.apache.tools.ant.types.Path
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import java.io.File

abstract class AbstractClassesTask : DefaultTask(), AemTask {

    abstract val sourceSet: SourceSet

    @Input
    final override val config = AemConfig.extend(project)

    @InputDirectory
    val classesDir: File = sourceSet.output.classesDir

    @OutputDirectory
    val osgiInfDir = File(classesDir, AemPlugin.OSGI_INF)

    @TaskAction
    fun processClasses() {
        if (!classesDir.exists()) {
            logger.info("Processing classes for source set '${sourceSet.name}' is skipped, because classes directory does not exist: $classesDir")
            return
        }

        logger.info("Processing classes in source set '${sourceSet.name}' at path '${classesDir.path}'")

        processServiceComponents()
    }

    private fun processServiceComponents() {
        if (!config.scrEnabled) {
            return
        }

        logger.info("Executing ant task to generate XML descriptors for service components")

        prepareDestDir()
        executeScrTask()

        val xmlFiles = osgiInfDir.listFiles().toList()
        logger.info("Generated ${xmlFiles.size} file(s) at path: ${osgiInfDir.absolutePath}")
    }

    private fun prepareDestDir() {
        osgiInfDir.deleteRecursively()
        osgiInfDir.mkdirs()
    }

    private fun executeScrTask(): SCRDescriptorTask {
        val antTask = SCRDescriptorTask()
        val antProject = project.ant.project
        val antClassPath = Path(antProject, sourceSet.runtimeClasspath.asPath)

        antTask.setSrcdir(classesDir)
        antTask.setDestdir(classesDir)
        antTask.setClasspath(antClassPath)
        antTask.setStrictMode(config.scrStrictMode)
        antTask.project = antProject

        if (!config.scrExcludes.isNullOrBlank()) {
            antTask.setExcludes(config.scrExcludes)
        }
        if (!config.scrIncludes.isNullOrBlank()) {
            antTask.setIncludes(config.scrIncludes)
        }
        if (!config.scrSpecVersion.isNullOrBlank()) {
            antTask.setSpecVersion(config.scrSpecVersion)
        }

        antTask.execute()

        return antTask
    }

    protected fun getSourceSet(name: String): SourceSet {
        return project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName(name)
    }

}