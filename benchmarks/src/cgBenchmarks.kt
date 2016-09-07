package org.jetbrains.dynatic.benchmark

import net.sf.cglib.proxy.*
import org.openjdk.jmh.annotations.*
import java.lang.reflect.*
import java.util.concurrent.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
open class CgBenchmark {
    inner class DelegateInterceptor(val source: Map<String, *>) : MethodInterceptor {
        override fun intercept(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any? {
            return when {
                method.name.startsWith("get") -> source[method.name.drop(3).decapitalize()]
                else -> throw UnsupportedOperationException("Cannot intercept method $method")
            }
        }
    }

    inner class DelegateInterceptorHardcoded(val source: Map<String, *>) : MethodInterceptor {
        override fun intercept(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any? {
            return when(method.name) {
                "getCount" -> source["count"]
                "getSize" -> source["size"]
                "getPercent" -> source["percent"]
                else -> throw UnsupportedOperationException("Cannot intercept method $method")
            }
        }
    }

    val map = mapOf("count" to 12, "size" to 42L, "percent" to 99.9)

    val mapper = { source: Map<String, *> ->
        Enhancer().apply {
            setInterfaces(arrayOf(Numbers::class.java))
            setCallback(DelegateInterceptor(source))
        }.create() as Numbers
    }

    val mapperHardcoded = { source: Map<String, *> ->
        Enhancer().apply {
            setInterfaces(arrayOf(Numbers::class.java))
            setCallback(DelegateInterceptorHardcoded(source))
        }.create() as Numbers
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun interceptor(): Double {
        val numbers = mapper(map)
        return numbers.count + numbers.size + numbers.percent
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun interceptorHardcoded(): Double {
        val numbers = mapperHardcoded(map)
        return numbers.count + numbers.size + numbers.percent
    }
}
