package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.common.instance.InstanceException

@Suppress("MagicNumber")
enum class Status(val exitStatus: Int) {
    RUNNING(0),
    DEAD(1),
    NOT_RUNNING(3),
    UNKNOWN(4);

    companion object {

        fun byScriptStatus(exitStatus: Int) = values().find { it.exitStatus == exitStatus }
                ?: throw InstanceException("Unrecognized local instance script exit status '$exitStatus'")

        fun of(name: String) = values().find { it.name.equals(name, true) }
                ?: throw InstanceException("Unsupported local instance status '$name'")
    }
}