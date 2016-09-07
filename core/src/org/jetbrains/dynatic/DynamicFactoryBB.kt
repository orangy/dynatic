package org.jetbrains.dynatic

import net.bytebuddy.*
import net.bytebuddy.description.annotation.*
import net.bytebuddy.description.method.*
import net.bytebuddy.description.type.*
import net.bytebuddy.implementation.*
import net.bytebuddy.implementation.bind.*
import net.bytebuddy.implementation.bind.annotation.*
import net.bytebuddy.implementation.bytecode.assign.*
import net.bytebuddy.implementation.bytecode.constant.*
import net.bytebuddy.matcher.*
import java.lang.reflect.*
import kotlin.reflect.*

fun <Interface : Any, Source : Any> getOrGenerateFactoryBB(interfaceKlass: KClass<Interface>, sourceKlass: KClass<Source>): Constructor<*> {
    val holderType = TypeDescription.Generic.Builder.parameterizedType(SourceHolder::class.java, sourceKlass.java).build()
    val buddyClass = ByteBuddy()
            .subclass<SourceHolder<Source>>(holderType)
            .implement(interfaceKlass.java)
            .method(ElementMatchers.any()).intercept(
            MethodDelegation.to(Interceptor<Source>())
                    .appendParameterBinder(FunctionNameBinder)
                    .appendParameterBinder(PropertyNameBinder)
                    .appendParameterBinder(PropertyTypeBinder))
            .make()
            .load(interfaceKlass.java.classLoader, net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.WRAPPER)
            .loaded
    val constructor = buddyClass.getConstructor(sourceKlass.java, DynamicAccessor::class.java)
    return constructor
}

private annotation class FunctionName()
private annotation class PropertyName()
private annotation class PropertyType()

private open class SourceHolder<Source : Any>(val source: Source, val accessor: DynamicAccessor<Source>)

private class Interceptor<Source : Any>() {
    @RuntimeType
    @BindingPriority(1)
    fun call(@This holder: SourceHolder<Source>, @FunctionName name: String, @PropertyType type: Type, @AllArguments args: Array<Any?>): Any? {
        return holder.accessor.callFunction(holder.source, name, type, args)
    }

    @RuntimeType
    @BindingPriority(2)
    fun get(@This holder: SourceHolder<Source>, @PropertyName name: String, @PropertyType type: Type): Any? {
        return holder.accessor.getProperty(holder.source, name, type)
    }

    @RuntimeType
    @BindingPriority(2)
    fun set(@This holder: SourceHolder<Source>, @PropertyName name: String, @PropertyType type: Type, @Argument(0) value: Any?): Any? {
        return holder.accessor.setProperty(holder.source, name, type, value)
    }
}

private object PropertyNameBinder : TargetMethodAnnotationDrivenBinder.ParameterBinder<PropertyName> {
    override fun getHandledType(): Class<PropertyName> = PropertyName::class.java
    override fun bind(annotation: AnnotationDescription.Loadable<PropertyName>, source: MethodDescription, target: ParameterDescription, implementationTarget: Implementation.Target, assigner: Assigner): MethodDelegationBinder.ParameterBinding<*> {
        if (!target.type.asErasure().represents(String::class.java)) {
            throw IllegalStateException("$target makes illegal use of @PropertyName")
        }
        val name = when {
            source.name.startsWith("get") -> source.name.drop(3).decapitalize()
            source.name.startsWith("set") -> source.name.drop(3).decapitalize()
            else -> return net.bytebuddy.implementation.bind.MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE
        }
        return net.bytebuddy.implementation.bind.MethodDelegationBinder.ParameterBinding.Anonymous(TextConstant(name))
    }
}

private object FunctionNameBinder : TargetMethodAnnotationDrivenBinder.ParameterBinder<FunctionName> {
    override fun getHandledType(): Class<FunctionName> = FunctionName::class.java
    override fun bind(annotation: AnnotationDescription.Loadable<FunctionName>, source: MethodDescription, target: ParameterDescription, implementationTarget: Implementation.Target, assigner: Assigner): MethodDelegationBinder.ParameterBinding<*> {
        if (!target.type.asErasure().represents(String::class.java)) {
            throw IllegalStateException("$target makes illegal use of @FunctionName")
        }
        return net.bytebuddy.implementation.bind.MethodDelegationBinder.ParameterBinding.Anonymous(TextConstant(source.name))
    }
}

private object PropertyTypeBinder : TargetMethodAnnotationDrivenBinder.ParameterBinder<PropertyType> {
    override fun getHandledType(): Class<PropertyType> = PropertyType::class.java
    override fun bind(annotation: AnnotationDescription.Loadable<PropertyType>, source: MethodDescription, target: ParameterDescription, implementationTarget: Implementation.Target, assigner: Assigner): MethodDelegationBinder.ParameterBinding<*> {
        if (!target.type.asErasure().represents(Type::class.java)) {
            throw IllegalStateException("$target makes illegal use of @PropertyType")
        }
        return MethodDelegationBinder.ParameterBinding.Anonymous(ClassConstant.of(source.returnType.asErasure()))
    }
}

