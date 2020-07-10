package com.cognifide.gradle.sling.common.instance.check

class CustomCheck(group: CheckGroup, private val callback: CustomCheck.() -> Unit) : DefaultCheck(group) {

    override fun check() {
        callback()
    }
}
