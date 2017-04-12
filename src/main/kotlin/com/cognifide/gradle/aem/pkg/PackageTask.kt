package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.util.*

/**
 * TODO Input is also effect of SCR plugin / metadata?
 * TODO Remove effective* fields and implement config decorator which will be placed as field for each task
 */
abstract class PackageTask : Zip() {

 //   @Input
    var expandProperties = mutableMapOf<String, String>()

 //   @Input
    var vaultCommonPath = ""

 //   @Input
    var vaultProfilePath = ""

 //   @Input
    var fileExpands = mutableListOf<String>()

//    @Input
    var fileIgnores = mutableListOf<String>()

//    @Input
    var contentPath = ""

  //  @Input
    var bundlePath = ""

 //   @Internal
    var bundleCollectors: List<() -> List<File>> = mutableListOf()

 //   @Internal
    protected val config: AemConfig = project.extensions.getByType(AemConfig::class.java)

    init {
        group = AemPlugin.TASK_GROUP
        duplicatesStrategy = DuplicatesStrategy.WARN
        expandProperties.put("assembly.name", project.rootProject.name)
    }

    @TaskAction
    override fun copy() {
        compose()
        super.copy()
    }

    protected open fun compose() {
        copyBundles(collectBundles())
    }

    fun assemble(project: Project, taskDeps: List<String> = listOf()) {
        includeContent(project)

        bundleCollectors += { collectBundles(project) }

        var deps = taskDeps
        if (deps.isEmpty()) {
            deps = if (project == this.project) listOf() else listOf(LifecycleBasePlugin.BUILD_TASK_NAME)
        }
        deps.forEach { taskName -> dependsOn("${project.path}:${taskName}") }
    }

    open val effectiveFileExpands
        get() = config.fileExpands + fileExpands

    open val effectiveFileIgnores
        get() = config.fileIgnores + fileIgnores

    open val effectiveFileExpandProperties
        get() = project.properties + config.expandProperties + expandProperties

    open val effectiveBundlePath
        get() = arrayOf(this.bundlePath, config.bundlePath, "jcr_root/apps/" + project.rootProject.name + "/install")
                .filter { !it.isNullOrBlank() }.first()

    fun determineContentPath(project: Project) : String {
        return project.projectDir.path + "/" + arrayOf(contentPath, config.contentPath).filter { !it.isNullOrBlank() }.first()
    }

    open fun includeContent(project: Project) {
        val contentDir = File(determineContentPath(project))
        if (!contentDir.exists()) {
            logger.info("Package JCR root directory does not exist: ${contentDir.absolutePath}")
        }

        from(contentDir, {
         //   exclude(effectiveFileIgnores)
        //    exclude(this.effectiveFileExpands)
        })

//        from(contentDir, {
//            exclude(effectiveFileIgnores)
//            include(this.effectiveFileExpands)
//            filter { line -> replaceProperties(line) }
//            expand(mapOf("project" to project.properties))
//        })
    }

    open fun includeProfile(profileName: String) {
        val commonPath = arrayOf(vaultCommonPath, config.vaultCommonPath).filter { !it.isNullOrBlank() }.first()
        val profilePath = arrayOf(vaultProfilePath, config.vaultProfilePath).filter { !it.isNullOrBlank() }.first()

        includeVault(project.relativePath(commonPath))
        includeVault(project.relativePath(profilePath + "/" + profileName))
    }

    open fun includeVault(vltPath: Any) {
        into(AemPlugin.VLT_PATH) {
            from(vltPath) {
                exclude(this.effectiveFileExpands)
            }
            from(vltPath) {
                include(this.effectiveFileExpands)
                filter { line -> replaceProperties(line) }
                expand(mapOf("project" to project.properties))
            }
        }
    }

    protected open fun replaceProperties(content: String) = StrSubstitutor.replace(content, effectiveFileExpandProperties)

    protected open fun collectBundles(project: Project) = JarCollector(project).all.toList()

    protected open fun collectBundles(): List<File> {
        return bundleCollectors.fold(TreeSet<File>(), { files, it -> files.addAll(it()); files }).toList()
    }

    protected fun copyBundles(jars: Collection<File>) {
        if (!jars.isEmpty()) {
            logger.info("Copying bundles into AEM package: " + jars.toString())
            into(effectiveBundlePath) { spec -> spec.from(jars) }
        }
    }
}
