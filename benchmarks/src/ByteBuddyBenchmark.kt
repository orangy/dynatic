package org.jetbrains.dynatic.benchmark

import net.bytebuddy.*
import net.bytebuddy.description.annotation.*
import net.bytebuddy.description.method.*
import net.bytebuddy.implementation.*
import net.bytebuddy.implementation.bind.*
import net.bytebuddy.implementation.bind.annotation.*
import net.bytebuddy.implementation.bytecode.assign.*
import net.bytebuddy.implementation.bytecode.constant.*
import net.bytebuddy.matcher.*
import org.openjdk.jmh.annotations.*
import java.lang.reflect.*
import java.util.concurrent.*

annotation class PropertyName()
annotation class PropertyType()


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
open class ByteBuddyBenchmark {
    open class SourceHolder(val map: Map<String, *>)

    val buddy = ByteBuddy()
            .subclass(SourceHolder::class.java)
            .implement(Numbers::class.java)
            .method(ElementMatchers.any()).intercept(
                MethodDelegation.to(Interceptor())
                        .appendParameterBinder(PropertyNameBinder.INSTANCE)
                        .appendParameterBinder(PropertyTypeBinder.INSTANCE))
            .make()
            .load(Numbers::class.java.classLoader, net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.WRAPPER)
            .loaded
            .getConstructor(Map::class.java)

    enum class PropertyNameBinder : TargetMethodAnnotationDrivenBinder.ParameterBinder<PropertyName> {

        INSTANCE;
        // singleton

        override fun getHandledType(): Class<PropertyName> = PropertyName::class.java
        override fun bind(annotation: AnnotationDescription.Loadable<PropertyName>, source: MethodDescription, target: ParameterDescription, implementationTarget: Implementation.Target, assigner: Assigner): MethodDelegationBinder.ParameterBinding<*> {
            if (!target.type.asErasure().represents(String::class.java)) {
                throw IllegalStateException("$target makes illegal use of @PropertyName")
            }
            val name = when {
                source.name.startsWith("get") -> source.name.drop(3).decapitalize()
                source.name.startsWith("set") -> source.name.drop(3).decapitalize()
                else -> return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE
            }
            return MethodDelegationBinder.ParameterBinding.Anonymous(TextConstant(name))
        }
    }

    enum class PropertyTypeBinder : TargetMethodAnnotationDrivenBinder.ParameterBinder<PropertyType> {

        INSTANCE;
        // singleton

        override fun getHandledType(): Class<PropertyType> = PropertyType::class.java
        override fun bind(annotation: AnnotationDescription.Loadable<PropertyType>, source: MethodDescription, target: ParameterDescription, implementationTarget: Implementation.Target, assigner: Assigner): MethodDelegationBinder.ParameterBinding<*> {
            if (!target.type.asErasure().represents(Type::class.java)) {
                throw IllegalStateException("$target makes illegal use of @PropertyType")
            }
            return MethodDelegationBinder.ParameterBinding.Anonymous(ClassConstant.of(source.returnType.asErasure()))
        }
    }

    class Interceptor() {
        @RuntimeType
        fun invoke(@This source: SourceHolder, @PropertyName property: String, @PropertyType type: Type): Any? {
            return source.map[property]
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
    val byteBuddyBenchmark = ByteBuddyBenchmark()
    val numbers = byteBuddyBenchmark.run { buddy.newInstance(map) } as Numbers
    val x = numbers.count + numbers.size + numbers.percent
    println(numbers.count)
}
