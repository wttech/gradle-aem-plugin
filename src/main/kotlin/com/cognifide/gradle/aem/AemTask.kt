package com.cognifide.gradle.aem

/**
 * @since 1.3  Task configuration cannot be directly modified, because was confusing for plugin users.
 *
 * Instead: aemCompose { config { /* ... */ } }
 * Simply write: aem { config { /* ... */ } }
 */
interface AemTask {

    val config: AemConfig

}