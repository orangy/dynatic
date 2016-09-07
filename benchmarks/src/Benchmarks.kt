package org.jetbrains.dynatic.benchmark

import org.jetbrains.dynatic.*
import org.openjdk.jmh.annotations.*
import java.lang.reflect.*
import java.util.concurrent.*


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
open class MapBenchmarkGen {
    val map = mapOf("count" to 12, "size" to 42L, "percent" to 99.9)

    init {
        emittedWrappers.clear()
    }

    val mapper = implementDynamic(Numbers::class, Map::class, MapAccessor)

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun dynamicGenerateMapper(): Double {
        val numbers = mapper(map)
        return numbers.count + numbers.size + numbers.percent
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun dynamicGenerateEach(): Double {
        val numbers = implementDynamic(Numbers::class, Map::class, MapAccessor)(map)
        //val numbers = map.mapCast<Numbers>()
        return numbers.count + numbers.size + numbers.percent
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun dynamicGenerateInline(): Double {
        val numbers = map.mapCast<Numbers>()
        return numbers.count + numbers.size + numbers.percent
    }
}

inline fun <reified V : Any> Map<String, Any?>.mapCast(): V = implementDynamic<V, Map<String, Any?>>(MapAccessor)(this)

object MapAccessor : DynamicAccessor<Map<String, Any?>> {
    override fun getProperty(source: Map<String, Any?>, name: String, type: Type): Any? {
        return source[name] ?: throw IllegalAccessException("property $name not found")
    }

    override fun setProperty(source: Map<String, Any?>, name: String, type: Type, value: Any?) = TODO()
    override fun callFunction(source: Map<String, Any?>, name: String, type: Type, values: Array<Any?>): Any? = TODO()
}
