package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.environment.docker.Stack

open class DockerTask : AemDefaultTask() {

    protected val stack = Stack(aem)
}