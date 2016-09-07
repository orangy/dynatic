package org.jetbrains.dynatic.benchmark

import net.sf.cglib.proxy.*
import org.openjdk.jmh.annotations.*
import java.lang.reflect.*
import java.util.concurrent.*


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
open class CgLibBenchmarks {
    open class SourceHolder(val map: Map<String, *>)

    val enhancer = Enhancer().apply {
        setSuperclass(SourceHolder::class.java)
        setInterfaces(arrayOf(Numbers::class.java))
        setCallback(DelegateInterceptor())
    }

    class DelegateInterceptor() : MethodInterceptor {
        override fun intercept(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any? {
            val source = (obj as SourceHolder).map
            return when {
                method.name.startsWith("get") -> source[method.name.drop(3).decapitalize()]
                else -> throw UnsupportedOperationException("Cannot intercept method $method")
            }
        }
    }


    val map = mapOf("count" to 12, "size" to 42L, "percent" to 99.9)

    val mapper = { source: Map<String, *> ->
        enhancer.create(arrayOf(Map::class.java), arrayOf(source)) as Numbers
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun interceptor(): Double {
        val numbers = mapper(map)
        return numbers.count + numbers.size + numbers.percent
    }

    val enhancerHardcoded = Enhancer().apply {
        setSuperclass(SourceHolder::class.java)
        setInterfaces(arrayOf(Numbers::class.java))
        setCallback(DelegateInterceptorHardcoded())
    }

    class DelegateInterceptorHardcoded() : MethodInterceptor {
        override fun intercept(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any? {
            val source = (obj as SourceHolder).map
            return when (method.name) {
                "getCount" -> source["count"]
                "getSize" -> source["size"]
                "getPercent" -> source["percent"]
                else -> throw UnsupportedOperationException("Cannot intercept method $method")
            }
        }
    }

    val mapperHardcoded = { source: Map<String, *> ->
        enhancerHardcoded.create(arrayOf(Map::class.java), arrayOf(source)) as Numbers
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun interceptorHardcoded(): Double {
        val numbers = mapperHardcoded(map)
        return numbers.count + numbers.size + numbers.percent
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun interceptorHardcodedDirect(): Double {
        val numbers = enhancerHardcoded.create(arrayOf(Map::class.java), arrayOf(map)) as Numbers
        return numbers.count + numbers.size + numbers.percent
    }
}
