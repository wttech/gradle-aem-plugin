package com.cognifide.gradle.aem.common.instance.local

@Suppress("MagicNumber")
class Status(val type: Type, val exitValue: String) {

    val text: String get() = when (type) {
        Type.UNRECOGNIZED -> "${type.displayName} ($exitValue)"
        else -> type.displayName
    }

    val running: Boolean get() = type == Type.RUNNING

    val runnable: Boolean get() = Type.RUNNABLE.contains(type)

    val unrecognized: Boolean get() = type == Type.UNRECOGNIZED

    @Suppress("MagicNumber")
    enum class Type(val exitValue: String) {
        RUNNING("0"),
        DEAD("1"),
        NOT_RUNNING("3"),
        UNKNOWN("4"),
        UNRECOGNIZED("<none>");

        val displayName: String get() = name.toLowerCase().replace("_", " ").capitalize()

        companion object {
            val RUNNABLE = arrayOf(NOT_RUNNING, UNKNOWN)

            fun byExitValue(exitValue: Int) = values().find { it.exitValue == exitValue.toString() } ?: UNRECOGNIZED
        }
    }

    override fun toString() = text

    companion object {
        val UNRECOGNIZED = Status(Type.UNRECOGNIZED, Type.UNRECOGNIZED.exitValue)

        fun byExitValue(exitValue: Int) = Status(Type.byExitValue(exitValue), exitValue.toString())
    }
}
