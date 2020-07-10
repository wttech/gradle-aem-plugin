package com.cognifide.gradle.sling.test

import com.cognifide.gradle.common.utils.using
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

open class SlingTest {

    fun usingProject(callback: Project.() -> Unit) = ProjectBuilder.builder().build().using(callback)
}
