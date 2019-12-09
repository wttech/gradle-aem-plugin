package com.cognifide.gradle.aem.common.build

import org.gradle.api.Project
import kotlin.reflect.KClass

open class InternalApi(val project: Project) {

    val serviceFactory: Any get() = invoke(project, "getServices")

    @Suppress("unchecked_cast")
    fun <T : Any> service(clazz: KClass<T>): T = invoke(serviceFactory, "get", clazz.java) as T

    @Suppress("SpreadOperator")
    operator fun invoke(obj: Any, method: String, vararg args: Any): Any {
        val argumentTypes = arrayOfNulls<Class<*>>(args.size)
        for (i in args.indices) {
            argumentTypes[i] = args[i].javaClass
        }
        val m = obj.javaClass.getMethod(method, *argumentTypes)
        m.isAccessible = true

        return m.invoke(obj, *args)
    }

    @Suppress("SpreadOperator")
    fun invoke(obj: Any, method: String, args: List<Any?>, argTypes: List<Class<out Any>>): Any {
        val m = obj.javaClass.getMethod(method, *argTypes.toTypedArray())
        m.isAccessible = true

        return m.invoke(obj, *args.toTypedArray())
    }
}
