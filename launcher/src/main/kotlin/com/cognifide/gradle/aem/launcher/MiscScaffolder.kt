package com.cognifide.gradle.aem.launcher

class MiscScaffolder(private val launcher: Launcher) {

    fun scaffold() {
        appendGitIgnore()
    }

    private fun appendGitIgnore() = launcher.workFile(".gitignore") {
        val content = """
            ### Gradle/GAP ###
            .gradle/
            build/
            /gap.jar
            dispatcher/src/conf.d/variables/default.vars
        """.trimIndent()

        if (!exists()) {
            println("Saving Git ignore file '$this'")
            writeText("${launcher.eol}${content}${launcher.eol}")
        } else if (!readText().contains(content)) {
            println("Appending lines to Git ignore file '$this'")
            appendText("${launcher.eol}${content}${launcher.eol}")
        }
    }
}