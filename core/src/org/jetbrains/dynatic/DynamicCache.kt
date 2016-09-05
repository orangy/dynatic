package org.jetbrains.dynatic

import java.lang.reflect.*
import java.util.concurrent.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

val emittedWrappers: ConcurrentMap<Pair<Class<*>, Class<*>>, Constructor<*>> = ConcurrentHashMap<Pair<Class<*>, Class<*>>, Constructor<*>>()
private val emitClassLoaders: ConcurrentMap<ClassLoader, EmitClassLoader> = ConcurrentHashMap()

internal class EmitClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    fun defineClass(name: String, b: ByteArray): Class<*> = defineClass(name, b, 0, b.size)
}

inline fun <reified Interface : Any, reified Source : Any> implementDynamic(accessor: DynamicAccessor<Source>): (Source) -> Interface {
    return implementDynamic(Interface::class, Source::class, accessor)
}

@Suppress("UNCHECKED_CAST")
fun <Interface : Any, Source : Any> implementDynamic(interfaceKlass: KClass<Interface>, sourceKlass: KClass<Source>, accessor: DynamicAccessor<*>): (Source) -> Interface {
    val factory = getOrGenerateFactory(interfaceKlass, sourceKlass)
    return { source -> factory.newInstance(source, accessor) as Interface }

    // Somehow the manually generated factory is slower than VM generated
    //return factory.newInstance(accessor) as (Source) -> Interface
}

fun <Interface : Any, Source : Any> getOrGenerateFactory(interfaceKlass: KClass<Interface>, sourceKlass: KClass<Source>): Constructor<*> {
    return emittedWrappers.computeIfAbsent(interfaceKlass.java to sourceKlass.java) {
        require(interfaceKlass.java.isInterface) { "Dynamic type should be interface, but is $interfaceKlass" }
        val prototypeFQN = interfaceKlass.jvmName
        val sourceFQN = sourceKlass.jvmName
        val generateKlass = "$prototypeFQN\$delegate\$${sourceKlass.simpleName}"
        val emitter = DynamicGenerator(generateKlass.replace('.', '/'), sourceFQN.replace('.', '/'))
        emitter.begin(interfaceKlass.qualifiedName!!.replace('.', '/'))
        for (property in interfaceKlass.memberProperties) {
            val propertyName = property.name
            val propertyType = org.objectweb.asm.Type.getReturnType(property.getter.javaMethod)

            emitter.getProperty(interfaceKlass, propertyName, propertyType)
            if (property is KMutableProperty1)
                emitter.setProperty(interfaceKlass, propertyName, propertyType)
        }
        for (function in interfaceKlass.memberFunctions) {
            val propertyName = function.name
            val method = function.javaMethod
            if (method != null) {
                val propertyType = org.objectweb.asm.Type.getReturnType(method)
                emitter.function(interfaceKlass, propertyName, propertyType, method.parameterTypes.map { org.objectweb.asm.Type.getType(it) })
            }
        }
        emitter.end()

        val parentClassLoader = interfaceKlass.java.classLoader
        val classLoader = emitClassLoaders.computeIfAbsent(parentClassLoader) {
            EmitClassLoader(parentClassLoader)
        }
        val implementationClass = classLoader.defineClass(generateKlass, emitter.getBytes())
        implementationClass.getConstructor(sourceKlass.java, DynamicAccessor::class.java)

/*
      // Somehow the manually generated factory is slower than VM generated
        val factoryClass = generateFactory(classLoader, generateKlass, sourceFQN.replace('.', '/'))
        factoryClass.getConstructor(DynamicAccessor::class.java)
*/
    }
}