package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.aem
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskAction

open class MvnExec : Exec() {

    val aem = project.aem

    @TaskAction
    override fun exec() {
        if (!workingDir.exists()) {
            throw MvnException("Cannot run Maven build at non-existing directory: '$workingDir'!")
        }
        super.exec()
    }

    init {
        val overridableArgs = (aem.prop.string("mvn.execArgs")?.split(" ") ?: listOf("-B"))
        val forcedArgs = listOf("-N")
        args = forcedArgs + overridableArgs
    }
}
