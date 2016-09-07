package org.jetbrains.dynatic.benchmark

import net.bytebuddy.*
import net.bytebuddy.implementation.*
import net.bytebuddy.matcher.*
import org.openjdk.jmh.annotations.*
import java.lang.reflect.*
import java.util.concurrent.*


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
open class ByteBuddyBenchmark {
    open class SourceHolder(val map: Map<String, *>)

    val buddy = ByteBuddy()
            .subclass(SourceHolder::class.java)
            .implement(Numbers::class.java)
            .method(ElementMatchers.isDeclaredBy(Numbers::class.java))
            .intercept(InvocationHandlerAdapter.of(ProxyInvocationHandler()))
            .make()
            .load(Numbers::class.java.classLoader, net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.WRAPPER)
            .loaded
            .getConstructor(Map::class.java)


    class ProxyInvocationHandler() : InvocationHandler {
        override fun invoke(obj: Any, method: Method, args: Array<out Any>): Any? {
            val source = (obj as SourceHolder).map
            return when {
                method.name.startsWith("get") -> source[method.name.drop(3).decapitalize()]
                else -> throw UnsupportedOperationException("Cannot intercept method $method")
            }
        }
    }

    val map = mapOf("count" to 12, "size" to 42L, "percent" to 99.9)
    val mapper = { source: Map<String, *> -> buddy.newInstance(source) as Numbers }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    @Benchmark
    open fun interceptor(): Double {
        val numbers = mapper(map)
        return numbers.count + numbers.size + numbers.percent
    }
}

fun main(args: Array<String>) {
    val numbers = ByteBuddyBenchmark().run {
        buddy.newInstance(map)
    } as Numbers
    println(numbers.count)
}
