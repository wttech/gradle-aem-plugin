package com.cognifide.gradle.aem.instance

import org.gradle.api.Project
import org.gradle.api.tasks.Input

class LocalHandleOptions(project: Project) {

    /**
     * Path from which extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    @Input
    var overridesPath: String = "${project.rootProject.file("src/main/resources/${InstancePlugin.FILES_PATH}")}"

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    @Input
    var expandFiles: List<String> = listOf("**/*.properties", "**/*.sh", "**/*.bat", "**/*.xml", "**/start", "**/stop")

    /**
     * Custom properties that can be injected into instance files.
     */
    @Input
    var expandProperties: Map<String, Any> = mapOf()
}