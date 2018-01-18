package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemConfig
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.http.client.config.RequestConfig
import org.gradle.api.Project

class InstanceState(val project: Project, val instance: Instance) {

    val config = AemConfig.of(project)

    val bundleState by lazy {
        InstanceSync(project, instance).determineBundleState({ method ->
            method.config = RequestConfig.custom()
                    .setConnectTimeout(config.awaitTimeout)
                    .setSocketTimeout(config.awaitTimeout)
                    .build()
        })
    }

    val stable: Boolean
        get() = bundleState.stable

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(instance)
                .append(bundleState)
                .toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstanceState

        return EqualsBuilder()
                .append(instance, other.instance)
                .append(bundleState, other.bundleState)
                .isEquals
    }

}