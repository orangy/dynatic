package org.jetbrains.dynatic

import java.util.concurrent.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

private val emittedWrappers: ConcurrentMap<Pair<KClass<*>, KClass<*>>, (Any, Any) -> Any> = ConcurrentHashMap<Pair<KClass<*>, KClass<*>>, (Any, Any) -> Any>()
private val emitClassLoaders: ConcurrentMap<ClassLoader, EmitClassLoader> = ConcurrentHashMap()

internal class EmitClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    fun defineClass(name: String, b: ByteArray): Class<*> = defineClass(name, b, 0, b.size)
}

inline fun <reified Interface : Any, reified Source : Any> implementDynamic(accessor: DynamicAccessor<Source>): (Source) -> Interface {
    val factory = getOrCreateDynamic(Interface::class, Source::class) as (Source, DynamicAccessor<Source>) -> Interface
    return { factory(it, accessor) }
}

fun implementDynamic(interfaceKlass: KClass<*>, sourceKlass: KClass<*>, accessor: DynamicAccessor<*>): (Any) -> Any {
    val factory = getOrCreateDynamic(interfaceKlass, sourceKlass)
    return { factory(it, accessor) }
}

fun <Interface : Any, Source : Any> getOrCreateDynamic(interfaceKlass: KClass<Interface>, sourceKlass: KClass<Source>): (Any, Any) -> Any {
    require(interfaceKlass.java.isInterface) { "Dynamic type should be interface, but is $interfaceKlass" }
    val parentClassLoader = interfaceKlass.java.classLoader
    val classLoader = emitClassLoaders.computeIfAbsent(parentClassLoader) {
        EmitClassLoader(parentClassLoader)
    }

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

        val implementationKlass = classLoader.defineClass(generateKlass, emitter.getBytes())
        if (useGeneratedFactory) {
            val factoryClass = generateFactory(classLoader, generateKlass, sourceFQN.replace('.', '/'))
            val factory = factoryClass.getConstructor().newInstance() as (Any, Any) -> Any
            factory
        } else {
            val implementationConstructor = implementationKlass.getConstructor(sourceKlass.java, DynamicAccessor::class.java)
            val ctorFactory: (Any, Any) -> Any = { source, accessor -> implementationConstructor.newInstance(source, accessor) }
            ctorFactory
        }
    }
}

var useGeneratedFactory = false
