package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.aem
import org.gradle.api.tasks.Exec

open class MvnExec : Exec() {

    val aem = project.aem

    init {
        val overridableArgs = (aem.prop.string("mvn.execArgs")?.split(" ") ?: listOf())
        val forcedArgs = listOf("-N")
        args = forcedArgs + overridableArgs
    }
}
