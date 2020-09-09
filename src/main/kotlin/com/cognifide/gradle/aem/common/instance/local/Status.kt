package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.common.instance.LocalInstanceException

@Suppress("MagicNumber")
enum class Status(val exitStatus: Int) {
    RUNNING(0),
    DEAD(1),
    NOT_RUNNING(3),
    UNKNOWN(4);

    val displayName: String get() = name.toLowerCase().replace("_", " ").capitalize()

    companion object {

        fun findByExitValue(code: Int) = values().find { it.exitStatus == code }

        fun getByExitValue(code: Int) = findByExitValue(code)
                ?: throw LocalInstanceException("Unrecognized local instance script exit value '$code'")

        fun of(name: String) = values().find { it.name.equals(name, true) }
                ?: throw LocalInstanceException("Unsupported local instance status '$name'")
    }
}
