package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.instance.InstanceAction
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

abstract class AbstractAction(val project: Project) : InstanceAction {

    val config = AemConfig.of(project)

    val logger = project.logger

    fun configure(closure: Closure<*>) {
        ConfigureUtil.configure(closure, this)
    }

}