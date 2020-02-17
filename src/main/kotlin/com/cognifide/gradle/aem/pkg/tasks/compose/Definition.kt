package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.common.pkg.vault.VaultDefinition

class Definition(private val vaultDefinition: VaultDefinition) {

    val group get() = vaultDefinition.group.get()

    val name get() = vaultDefinition.name.get()

    val version get() = vaultDefinition.version.get()

    val description get() = vaultDefinition.description.orNull

    val createdBy get() = vaultDefinition.createdBy.orNull

    val properties get() = vaultDefinition.properties.get()

    val filters get() = vaultDefinition.filters // TODO lazy

    val nodeTypes get() = vaultDefinition.nodeTypes // TODO collection and lazy
}
