package com.cognifide.gradle.aem.common.pkg.validator

import net.adamcin.oakpal.core.InstallHookPolicy
import java.io.Serializable

class OakpalPlan: Serializable {

    var checklists = mutableListOf("net.adamcin.oakpal.core/basic")

    var installHookPolicy = InstallHookPolicy.SKIP

}
