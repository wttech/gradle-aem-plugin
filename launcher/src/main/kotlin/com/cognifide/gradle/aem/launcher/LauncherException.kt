package com.cognifide.gradle.aem.launcher

import org.gradle.api.GradleException

class LauncherException : GradleException {
    constructor(message: String) : super(message)
}
