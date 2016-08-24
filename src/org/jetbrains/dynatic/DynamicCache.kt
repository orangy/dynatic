package org.jetbrains.dynatic

import java.lang.reflect.*
import java.util.concurrent.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

private val emittedWrappers: ConcurrentMap<Pair<KClass<*>, KClass<*>>, Constructor<*>> = ConcurrentHashMap<Pair<KClass<*>, KClass<*>>, Constructor<*>>()
private val emitClassLoaders: ConcurrentMap<ClassLoader, EmitClassLoader> = ConcurrentHashMap()

private class EmitClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    fun defineClass(name: String, b: ByteArray): Class<*> = defineClass(name, b, 0, b.size)
}

inline fun <reified Interface : Any, reified Source : Any> implementDynamic(accessor: DynamicAccessor<Source>): (Source) -> Interface {
    return { getOrCreateDynamic(Interface::class, Source::class).newInstance(it, accessor) as Interface }
}

fun implementDynamic(interfaceKlass: KClass<*>, sourceKlass: KClass<*>, accessor: DynamicAccessor<*>): (Any) -> Any {
    return { getOrCreateDynamic(interfaceKlass, sourceKlass).newInstance(it, accessor) }
}

fun <Interface : Any, Source : Any> getOrCreateDynamic(interfaceKlass: KClass<Interface>, sourceKlass: KClass<Source>): Constructor<*> {
    require(interfaceKlass.java.isInterface) { "Dynamic type should be interface, but is $interfaceKlass" }
    return emittedWrappers.computeIfAbsent(interfaceKlass to sourceKlass) {
        val prototypeFQN = interfaceKlass.jvmName
        val sourceFQN = sourceKlass.jvmName
        val generateKlass = "$prototypeFQN\$delegate\$${sourceKlass.simpleName}"
        val emitter = DynamicGenerator(generateKlass.replace('.', '/'), sourceFQN.replace('.', '/'))
        emitter.begin(interfaceKlass.qualifiedName!!.replace('.', '/'))
        for (property in interfaceKlass.memberProperties) {
            val propertyName = property.name
            val propertyType = org.objectweb.asm.Type.getReturnType(property.getter.javaMethod)

            emitter.getProperty(propertyName, propertyType)
            if (property is KMutableProperty1)
                emitter.setProperty(propertyName, propertyType)
        }
        for (function in interfaceKlass.memberFunctions) {
            val propertyName = function.name
            val method = function.javaMethod
            if (method != null) {
                val propertyType = org.objectweb.asm.Type.getReturnType(method)
                emitter.function(propertyName, propertyType, method.parameterTypes.map { org.objectweb.asm.Type.getType(it) })
            }
        }
        emitter.end()

        val parentClassLoader = interfaceKlass.java.classLoader
        val classLoader = emitClassLoaders.computeIfAbsent(parentClassLoader) {
            EmitClassLoader(parentClassLoader)
        }

        classLoader
                .defineClass(generateKlass, emitter.getBytes())
                .getConstructor(sourceKlass.java, DynamicAccessor::class.java)
                .apply {
                    isAccessible = true
                }
    }
}