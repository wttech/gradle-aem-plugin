package com.cognifide.gradle.aem.pkg.tasks.compose

import org.gradle.api.artifacts.Dependency
import java.io.Serializable

class PackageDependency(val dependency: Dependency, val storagePath: String, val vaultFilter: Boolean) : Serializable
