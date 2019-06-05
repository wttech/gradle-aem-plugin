package com.cognifide.gradle.aem.common.instance

@Suppress("MagicNumber")
enum class InstanceStatus(val exitStatus: Int) {
    RUNNING(0),
    DEAD(1),
    NOT_RUNNING(3),
    UNKNOWN(4);

    companion object {

        fun of(exitStatus: Int): InstanceStatus = values().find { it.exitStatus == exitStatus } ?: UNKNOWN

        fun named(name: String): InstanceStatus? = values().find { it.name.equals(name, true) }
    }
}