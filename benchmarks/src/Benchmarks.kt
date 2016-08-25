package org.jetbrains.dynatic.benchmark

import org.jetbrains.dynatic.*
import org.openjdk.jmh.annotations.*
import java.lang.reflect.*
import java.util.concurrent.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
open class MapBenchmark {
    val map = mapOf("count" to 12, "size" to 42L, "percent" to 99.9)

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun dynamicGenerate(): Double {
        useGeneratedFactory = true
        val numbers = map.mapCast<Numbers>()
        return numbers.count + numbers.size + numbers.percent
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun dynamicReflection(): Double {
        useGeneratedFactory = false
        val numbers = map.mapCast<Numbers>()
        return numbers.count + numbers.size + numbers.percent
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    //@Benchmark
    open fun proxy(): Double {
        val klass = Numbers::class.java
        val numbers = Proxy.newProxyInstance(klass.classLoader, arrayOf(klass), MapInvocationHandler(map)) as Numbers
        return numbers.count + numbers.size + numbers.percent
    }
}

class MapInvocationHandler(val map: Map<String, Any?>) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        val methodName = method.name
        val propname = methodName.drop(3).decapitalize()
        return map[propname] ?: throw IllegalAccessException("property $methodName not found")
    }
}

interface Numbers {
    val count: Int
    var size: Long
    val percent: Double
}

inline fun <reified V : Any> Map<String, Any?>.mapCast(): V = implementDynamic<V, Map<String, Any?>>(MapAccessor)(this)

object MapAccessor : DynamicAccessor<Map<String, Any?>> {
    override fun getProperty(source: Map<String, Any?>, name: String, type: Type): Any? {
        return source[name] ?: throw IllegalAccessException("property $name not found")
    }

    override fun setProperty(source: Map<String, Any?>, name: String, type: Type, value: Any?) = TODO()
    override fun callFunction(source: Map<String, Any?>, name: String, type: Type, values: Array<Any?>): Any? = TODO()
}
