package com.cognifide.gradle.aem.test

import com.cognifide.gradle.common.utils.using
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

open class AemTest {

    fun usingProject(callback: Project.() -> Unit) = ProjectBuilder.builder().build().using(callback)
}
