package com.cognifide.gradle.sling.launcher

import org.gradle.api.GradleException

class LauncherException : GradleException {
    constructor(message: String) : super(message)
}