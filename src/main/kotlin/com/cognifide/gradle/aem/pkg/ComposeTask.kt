package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import com.fasterxml.jackson.databind.util.ISO8601Utils
import groovy.text.SimpleTemplateEngine
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import java.io.File
import java.util.*

/**
 * TODO Input is also effected by SCR plugin / metadata?
 */
open class ComposeTask : Zip(), AemTask {

    companion object {
        val NAME = "aemCompose"
    }

    @Internal
    var bundleCollectors: List<() -> List<File>> = mutableListOf()

    @Internal
    var contentCollectors: List<() -> Unit> = mutableListOf()

    @OutputFile
    val vaultPropertiesFile = File(project.buildDir, "${NAME}/${AemPlugin.VLT_PATH}/properties.xml")

    @Input
    override val config = AemConfig.extendFromGlobal(project)

    init {
        description = "Composes AEM / CRX package from JCR content and built JAR bundles."
        group = AemPlugin.TASK_GROUP

        duplicatesStrategy = DuplicatesStrategy.WARN

        // After this project configured
        project.afterEvaluate({
            includeProject(project)
            includeVaultProperties()
        })

        // After all projects configured
        project.gradle.projectsEvaluated({
            fromBundles()
            fromContents()
        })
    }

    @TaskAction
    override fun copy() {
        generateVaultProperties()
        super.copy()
    }

    private fun determineContentPath(project: Project): String {
        val task = project.tasks.getByName(ComposeTask.NAME) as ComposeTask

        return project.projectDir.path + "/" + task.config.contentPath
    }

    private fun fromBundles() {
        val jars = bundleCollectors.fold(TreeSet<File>(), { files, it -> files.addAll(it()); files }).toList()
        if (jars.isEmpty()) {
            logger.info("No bundles to copy into AEM package")
        } else {
            logger.info("Copying bundles into AEM package: " + jars.toString())
            into(config.bundlePath) { spec -> spec.from(jars) }
        }
    }

    private fun includeVaultProperties() {
        contentCollectors += {
            into(AemPlugin.VLT_PATH, {
                from("${project.buildDir}/${NAME}")
            })
        }
    }

    private fun generateVaultProperties() {
        val input = if (config.vaultPropertiesPath.isBlank()) {
            javaClass.getResourceAsStream("/${AemPlugin.VLT_PATH}/properties.xml")
        } else {
            val file = File(config.vaultPropertiesPath)
            if (!file.exists()) {
                throw PackageException("Vault properties template file does not exist: '${file.absolutePath}'")
            }

            file.inputStream()
        }

        val xml = try {
            expandProperties(input.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            throw PackageException("Cannot generate vault properties file. Probably some variables are not bound", e)
        }

        vaultPropertiesFile.parentFile.mkdirs()
        vaultPropertiesFile.printWriter().use { it.print(xml) }
    }

    private fun expandProperties(source: String): String {
        val props = System.getProperties().entries.fold(mutableMapOf<String, String>(), { map, entry ->
            map.put(entry.key.toString(), entry.value.toString()); map
        }) + config.vaultProperties
        val interpolated = StrSubstitutor.replace(source, props)
        val template = SimpleTemplateEngine().createTemplate(interpolated).make(mapOf(
                "project" to project,
                "config" to config,
                "created" to ISO8601Utils.format(Date())
        ))

        return template.toString()
    }

    private fun fromContents() {
        contentCollectors.onEach { it() }
    }

    fun includeProject(projectPath: String) {
        includeProject(project.findProject(projectPath))
    }

    fun includeProject(project: Project) {
        includeContent(project)
        includeBundles(project)

        dependsOn("${project.path}:${BasePlugin.ASSEMBLE_TASK_NAME}")
    }

    fun includeBundles(projectPath: String) {
        includeBundles(project.findProject(projectPath))
    }

    fun includeBundles(project: Project) {
        bundleCollectors += {
            JarCollector(project).all.toList()
        }
    }

    fun includeContent(projectPath: String) {
        includeContent(project.findProject(projectPath))
    }

    fun includeContent(project: Project) {
        contentCollectors += {
            val contentDir = File(determineContentPath(project))
            if (!contentDir.exists()) {
                logger.info("Package JCR content directory does not exist: ${contentDir.absolutePath}")
            } else {
                logger.info("Copying JCR content from: ${contentDir.absolutePath}")

                from(contentDir, {
                    exclude(config.fileIgnores)
                })
            }
        }
    }

    fun includeVault(vltPath: Any) {
        into(AemPlugin.VLT_PATH, {
            from(vltPath)
        })
    }

    fun includeVaultProfile(profileName: String) {
        includeVault(project.relativePath(config.vaultCommonPath))
        includeVault(project.relativePath(config.vaultProfilePath + "/" + profileName))
    }
}
