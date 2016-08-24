package org.jetbrains.dynatic

import java.lang.reflect.*

interface DynamicAccessor<in TSource> {
    fun getProperty(source: TSource, name: String, type: Type): Any?
    fun setProperty(source: TSource, name: String, type: Type, value: Any?)
    fun callFunction(source: TSource, name: String, type: Type, values: Array<Any?>): Any?
}