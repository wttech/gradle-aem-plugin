package com.cognifide.gradle.aem.pkg.tasks.compose

import org.gradle.api.artifacts.Dependency
import java.io.Serializable

class BundleDependency(val dependency: Dependency, val installPath: String, val vaultFilter: Boolean) : Serializable
