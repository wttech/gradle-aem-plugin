package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.docker.Stack

open class Docker : AemDefaultTask() {

    protected val stack = Stack(aem)
}