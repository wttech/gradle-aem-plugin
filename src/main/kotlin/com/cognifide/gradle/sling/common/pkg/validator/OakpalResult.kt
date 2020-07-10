package com.cognifide.gradle.sling.common.pkg.validator

@Suppress("MagicNumber")
enum class OakpalResult(val exitCode: Int) {
    UNKNOWN_ERROR(-1),
    SUCCESS(0),
    GENERAL_ERROR(1),
    ABORTED_SCAN(9),
    SEVERE_VIOLATION(10),
    MAJOR_VIOLATION(11),
    MINOR_VIOLATION(12);

    val cause get() = name.replace("_", " ").toLowerCase()

    companion object {
        fun byExitCode(value: Int) = values().find { it.exitCode == value } ?: UNKNOWN_ERROR
    }
}
