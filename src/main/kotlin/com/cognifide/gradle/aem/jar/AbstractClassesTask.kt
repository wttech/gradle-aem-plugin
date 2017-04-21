package com.cognifide.gradle.aem.jar

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemTask
import org.apache.felix.scrplugin.ant.SCRDescriptorTask
import org.apache.tools.ant.types.Path
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class AbstractClassesTask : DefaultTask(), AemTask {

    abstract val sourceSet: SourceSet

    override val config = AemConfig.extendFromGlobal(project)

    @InputDirectory
    val classesDir = sourceSet.output.classesDir

    @OutputDirectory
    val osgiInfDir = File(classesDir, "OSGI-INF")

    @TaskAction
    fun processClasses() {
        if (!classesDir.exists()) {
            logger.info("Processing classes for source set '${sourceSet.name}' is skipped, because classes directory does not exist: $classesDir")
            return
        }

        logger.info("Processing SCR annotations")
        executeScrTask()

        val xmlFiles = osgiInfDir.listFiles().toList()
        logger.info("Service component files generated (${xmlFiles.size})")
        xmlFiles.onEach { logger.info(it.absolutePath) }
    }

    private fun executeScrTask(): SCRDescriptorTask {
        val antTask = SCRDescriptorTask()
        val antProject = project.ant.project

        antTask.setSrcdir(classesDir)
        antTask.setDestdir(classesDir)
        antTask.setClasspath(Path(antProject, sourceSet.runtimeClasspath.asPath))
        antTask.setStrictMode(false)
        antTask.project = antProject
        antTask.isScanClasses = true

        antTask.execute()

        return antTask
    }

    protected fun getSourceSet(name: String): SourceSet {
        return project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName(name)
    }

}