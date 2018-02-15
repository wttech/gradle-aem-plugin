package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.internal.file.resolver.FileGroup
import com.cognifide.gradle.aem.internal.file.resolver.FileResolution
import com.cognifide.gradle.aem.internal.file.resolver.FileResolver
import groovy.lang.Closure
import java.io.File

class PackageGroup(resolver: FileResolver, name: String) : FileGroup(resolver, name) {

    var instance = "*"

    var await = true

    var reload = false

    private val firsts = mutableListOf<() -> Unit>()

    private val lasts = mutableListOf<() -> Unit>()

    private var awaitAction: (List<Instance>) -> Unit = await()

    private var reloadAction: (List<Instance>) -> Unit = reload()

    fun doFirst(action: () -> Unit) {
        firsts.add(action)
    }

    fun doLast(action: () -> Unit) {
        lasts.add(action)
    }

    fun await(closure: Closure<*> = Closure.IDENTITY): (List<Instance>) -> Unit {
        await = true
        awaitAction = { instances -> AwaitAction(resolver.project, instances).configure(closure) }

        return awaitAction
    }

    fun reload(): (List<Instance>) -> Unit {
        reload = true
        reloadAction = { instances -> instances.forEach { InstanceSync(resolver.project, it).reload() } }

        return reloadAction
    }

    fun afterSatisfy(instances: List<Instance>) {
        firsts.forEach { it() }
        if (await) {
            awaitAction(instances)
        }
        if (reload) {
            reloadAction(instances)
        }
        lasts.forEach { it() }
    }

    override fun createResolution(id: String, resolver: (FileResolution) -> File): FileResolution {
        return PackageResolution(this, id, resolver)
    }

}
