package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConstants
import com.cognifide.gradle.plugins.aem.pkg.bundle.JarEmbeder
import com.cognifide.gradle.plugins.aem.pkg.task.AssemblePackage
import com.cognifide.gradle.plugins.aem.pkg.task.CreatePackage
import org.apache.commons.lang.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin

class PackagePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(BasePlugin)
        project.extensions.create(PackageExtension.NAME, PackageExtension)

        project.task(CreatePackage.NAME, type: CreatePackage)
        project.task(AssemblePackage.NAME, type: AssemblePackage)

        createConfig(project, AemConstants.CONFIG_PROVIDE, JavaPlugin.COMPILE_CONFIGURATION_NAME, true)
        createConfig(project, AemConstants.CONFIG_INSTALL, JavaPlugin.COMPILE_CONFIGURATION_NAME, false)
        createConfig(project, AemConstants.CONFIG_EMBED, JavaPlugin.COMPILE_CONFIGURATION_NAME, false)

        project.plugins.withType(JavaPlugin) {
            project.jar.doFirst(new JarEmbeder())
        }
    }

    protected createConfig(Project project, String configName, String configToBeExtended, boolean transitive) {
        def config = project.configurations.create(configName, {
            it.transitive = transitive
        })
        forConfiguration(project, configToBeExtended, { extendedConfig ->
            extendedConfig.extendsFrom(config)
            appendConfigurationToCompileClasspath(project, config)
        })

        return config
    }

    protected forConfiguration(Project project, String name, Closure creator) {
        def config = project.configurations.findByName(name)
        if (config != null) {
            creator(config)
        } else {
            project.configurations.whenObjectAdded {
                if (it instanceof Configuration) {
                    config = (Configuration) it
                    if (StringUtils.equals(config.name, name)) {
                        creator(config)
                    }
                }
            }
        }
    }

    protected appendConfigurationToCompileClasspath(Project project, Configuration config) {
        project.sourceSets.main.compileClasspath += config
        project.sourceSets.test.compileClasspath += config
    }
}
