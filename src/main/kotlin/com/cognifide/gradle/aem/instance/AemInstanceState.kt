package com.cognifide.gradle.aem.instance

data class AemInstanceState(val instance: AemInstance, val bundleState: BundleState) {

    val stable: Boolean
        get() = bundleState.stable

}