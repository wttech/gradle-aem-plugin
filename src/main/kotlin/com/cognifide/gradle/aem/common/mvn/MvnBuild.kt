package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.utils.filterNotNull
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.utils.using
import org.gradle.api.Task
import org.gradle.api.UnknownProjectException
import java.util.*

class MvnBuild(val aem: AemExtension) {

    val project = aem.project

    val logger = project.logger

    val tasks get() = project.common.tasks

    val rootDir = aem.obj.dir {
        set(aem.project.projectDir)
    }

    fun rootDir(path: String) {
        rootDir.set(aem.project.file(path))
    }

    val rootPom = rootDir.file("pom.xml")

    val archetypePropertiesFile = aem.obj.file {
        set(rootDir.file("archetype.properties"))
    }

    val archetypeProperties = aem.obj.map<String, String> {
        set(archetypePropertiesFile.map { rf ->
            val rff = rf.asFile
            if (!rff.exists()) {
                throw MvnException(
                    listOf(
                        "Maven archetype properties file does not exist '$rff'!",
                        "Consider creating it or specify 'appId' and 'groupId' in build script."
                    ).joinToString(" ")
                )
            }

            Properties().apply { rff.bufferedReader().use { load(it) } }
                .filterNotNull()
                .entries.map { (k, v) -> k.toString() to v.toString() }
                .toMap()
        })
    }

    val appId = aem.obj.string {
        convention(archetypeProperties.getting("appId"))
        aem.prop.string("mvn.appId")?.let { set(it) }
    }

    val groupId = aem.obj.string {
        convention(archetypeProperties.getting("groupId"))
        aem.prop.string("mvn.groupId")?.let { set(it) }
    }

    val version
        get() = MvnGav.readFile(rootPom.get().asFile).version
            ?: throw MvnException("Cannot determine Maven build version at path '${rootDir.get().asFile}'!")

    val contentPath = aem.obj.string {
        convention("src/main/content")
        aem.prop.string("mvn.contentPath")?.let { set(it) }
    }

    val outputExclusions = aem.obj.strings {
        set(listOf(
            // ignored files
            "**/.idea/**",
            "**/.idea",
            "**/.gradle/**",
            "**/.gradle",
            "**/gradle.user.properties",
            "**/gradle/user/**",

            // build files
            "**/target/**",
            "**/target",
            "**/build/**",
            "**/build",
            "**/dist/**",
            "**/dist",
            "**/generated",
            "**/generated/**",

            "**/package-lock.json",

            // temporary files
            "**/node_modules/**",
            "**/node_modules",
            "**/node/**",
            "**/node",
            "**/*.log",
            "**/*.tmp",
        ))
    }

    val depGraph by lazy { DependencyGraph(this) }

    fun depGraph(options: DependencyGraph.() -> Unit) = depGraph.using(options)

    val moduleResolver by lazy { ModuleResolver(this) }

    fun moduleResolver(options: ModuleResolver.() -> Unit) = moduleResolver.using(options)

    val userDir = aem.obj.dir { set(project.file(System.getProperty("user.home"))) }

    val repositoryDir = userDir.map { it.dir(".m2/repository") }

    val modules = aem.obj.list<MvnModule> { convention(listOf()) }

    private var moduleOptions: MvnModule.() -> Unit = {}

    fun moduleOptions(options: MvnModule.() -> Unit) {
        this.moduleOptions = options
    }

    fun module(name: String, options: MvnModule.() -> Unit) {
        module(moduleResolver.byName(name), options)
    }

    private fun module(descriptor: ModuleDescriptor, options: MvnModule.() -> Unit) {
        project.project(descriptor.projectPath) { subproject ->
            subproject.plugins.apply(CommonPlugin::class.java)
            subproject.aem.common {
                tmpDir.set(project.layout.buildDirectory.dir("mvnBuild/${descriptor.artifactId}"))
            }
            MvnModule(this, descriptor, subproject).apply {
                moduleOptions()
                options()
            }.also { modules.add(it) }
        }
    }

    val init = aem.obj.boolean {
        set(aem.prop.flag("launcher.wrapper"))
    }

    fun discover() {
        val projectPaths = moduleResolver.projectPaths.get()
        val settingsFile = project.rootProject.file("settings.gradle.kts")
        val settingsLines = projectPaths.joinToString("\n") { """include("$it")""" }

        if (init.get()) {
            logger.info("Updating Gradle settings due discovered Maven build projects (${projectPaths.size})")
            settingsFile.appendText(settingsLines)
        } else {
            try {
                defineModulesResolved()
            } catch (e: UnknownProjectException) {
                throw MvnException(
                    listOf(
                        "Maven build at path '${rootDir.get().asFile}' needs subprojects defined in Gradle settings as prerequisite.",
                        "Ensure having following lines (${projectPaths.size}) in file: $settingsFile",
                        "",
                        settingsLines,
                        ""
                    ).joinToString("\n")
                )
            }

            defineModuleTaskDependenciesFromGraph()
            defineDeployPackageTask()
        }
    }

    fun defineModulesResolved() = moduleResolver.all.get().forEach { descriptor ->
        module(descriptor) {
            when (descriptor.type) {
                ModuleType.POM -> buildPom()
                ModuleType.JAR -> buildJar()
                ModuleType.PACKAGE -> configurePackage()
                ModuleType.FRONTEND -> buildFrontend()
                ModuleType.DISPATCHER -> buildZip()
                ModuleType.MODULE -> buildModule()
            }
        }
    }

    fun defineModuleTaskDependenciesFromGraph() = depGraph.all.get().forEach { dependency ->
        val module1 = moduleResolver.byArtifact(dependency.from)
        val module2 = moduleResolver.byArtifact(dependency.to)

        // Build ordering
        val buildTask1 = tasks.pathed<Task>(module1.artifactTaskPath)
        val buildTask2 = tasks.pathed<Task>(module2.artifactTaskPath)

        buildTask1.configure { bt1 ->
            if (!dependency.redundant) {
                bt1.dependsOn(buildTask2)
            }
            bt1.inputs.files(buildTask2.map { it.outputs.files })
        }

        // Package deploy ordering
        if (deployPackageOrder.get() == DeployPackageOrder.GRAPH) {
            if (module1.type == ModuleType.PACKAGE && module2.type == ModuleType.PACKAGE) {
                val deployTask1 = tasks.pathed<Task>(module1.taskPath(MvnModule.TASK_PACKAGE_DEPLOY))
                val deployTask2 = tasks.pathed<Task>(module2.taskPath(MvnModule.TASK_PACKAGE_DEPLOY))

                deployTask1.configure { dt1 ->
                    dt1.mustRunAfter(deployTask2)
                }
            }
        }
    }

    val deployPackageOrder = aem.obj.typed<DeployPackageOrder> {
        convention(DeployPackageOrder.PRECEDENCE)
        aem.prop.string("mvn.deployPackageOrder")?.let { set(DeployPackageOrder.of(it)) }
    }

    fun deployPackageOrder(type: String) {
        deployPackageOrder.set(DeployPackageOrder.of(type))
    }

    val deployPackagePrecedence = aem.obj.strings {
        convention(listOf(
            "prereqs",
            "prereqs.*",
            "prereqs-*",
            "ui.prereqs",
            "ui.prereqs.*",
            "ui.prereqs-*",
            "ui.apps.prereqs",
            "ui.apps-prereqs",
            "ui.apps",
            "ui.apps.*",
            "ui.apps-*",
            "ui.*.apps",
            "ui.*-apps",
            "config",
            "config.*",
            "config-*",
            "ui.config",
            "ui.config.*",
            "ui.config-*",
            "ui.*.config",
            "ui.*-config",
            "ui.content",
            "ui.content.*",
            "ui.content-*",
            "ui.*.content",
            "ui.*-content",
            "ui.*",
            "all.*",
            "all-*",
            "*.all",
            "*-all",
            "all"
        ))
        aem.prop.list("mvn.deployPackagePrecedence")?.let { set(it) }
    }

    val deployPackageNames = aem.obj.strings {
        convention(listOf(
            "*",
            "!all.*",
            "!all-*",
            "!*.all",
            "!*-all",
            "!all"
        ))
        aem.prop.list("mvn.deployPackageNames")?.let { set(it) }
    }

    fun defineDeployPackageTask() {
        val packageModules = moduleResolver.all.get()
            .filter { it.type == ModuleType.PACKAGE && Patterns.wildcard(it.name, deployPackageNames.get()) }
        val taskOptions: Task.() -> Unit = { description = "Deploys AEM packages incrementally" }

        if (deployPackageOrder.get() == DeployPackageOrder.PRECEDENCE) {
            val deployTasks = packageModules.sortedBy { module ->
                deployPackagePrecedence.get().indexOfFirst { Patterns.wildcard(module.name, it) }
            }.map { tasks.pathed<Task>(it.taskPath(MvnModule.TASK_PACKAGE_DEPLOY)) }
            tasks.registerSequence(MvnModule.TASK_PACKAGE_DEPLOY, taskOptions) { dependsOn(deployTasks) }
        } else {
            val deployTasks = packageModules.map { tasks.pathed<Task>(it.taskPath(MvnModule.TASK_PACKAGE_DEPLOY)) }
            tasks.register(MvnModule.TASK_PACKAGE_DEPLOY) { taskOptions(); dependsOn(deployTasks) }
        }
    }

    override fun toString() = "MvnBuild(rootDir=${rootDir.get().asFile}, appId=${appId.orNull}, groupId=${groupId.orNull})"
}
