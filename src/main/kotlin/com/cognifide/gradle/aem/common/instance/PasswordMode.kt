package com.cognifide.gradle.aem.common.instance

enum class PasswordMode {
    RESET_WHEN_DOWN,
    UPDATE_WHEN_UP,
    NEVER;

    companion object {
        fun of(name: String): PasswordMode {
            return values().find { it.name.equals(name, true) }
                ?: throw LocalInstanceException("Unsupported local instance password update mode named: $name")
        }
    }
}
