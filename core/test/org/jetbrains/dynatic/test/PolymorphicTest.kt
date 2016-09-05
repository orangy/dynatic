package org.jetbrains.dynatic.test

import org.jetbrains.dynatic.*
import org.junit.*
import java.lang.reflect.*
import kotlin.test.*

interface BaseEntity<T> {
    val value: List<T>
    var overrideValue: T
}

interface DerivedEntity : BaseEntity<String> {
    override var overrideValue: String
}

class PolymorphicTest {
    @Test fun interpretAsBase() {
        val value = listOf("something")
        val map = mapOf("value" to value)
        val entity = map.mapCast<DerivedEntity>()
        assertEquals(value, entity.value)

        val baseEntity: BaseEntity<String> = map.mapCast<DerivedEntity>()
        assertEquals(value, baseEntity.value)
    }

    @Test fun interpretOverride() {
        val value = "something"
        val map = mapOf("overrideValue" to value)
        val entity = map.mapCast<DerivedEntity>()
        assertEquals(value, entity.overrideValue)

        val baseEntity: BaseEntity<String> = map.mapCast<DerivedEntity>()
        assertEquals(value, baseEntity.overrideValue)
    }

    @Test fun interpretOverrideSet() {
        val value = "something"
        val map = mutableMapOf("overrideValue" to value)
        val entity = map.mapCast<DerivedEntity>()
        assertEquals(value, entity.overrideValue)

        val baseEntity: BaseEntity<String> = map.mutableMapCast<DerivedEntity>()
        baseEntity.overrideValue = "nothing"
        assertEquals("nothing", baseEntity.overrideValue)
    }
}

inline fun <reified V : Any> MutableMap<String, String>.mutableMapCast(): V = (implementDynamic<V, MutableMap<String, String>>(MutableMapAccessor))(this)

object MutableMapAccessor : org.jetbrains.dynatic.DynamicAccessor<MutableMap<String, String>> {
    override fun getProperty(source: MutableMap<String, String>, name: String, type: Type): Any? {
        return source[name]
    }

    override fun setProperty(source: MutableMap<String, String>, name: String, type: Type, value: Any?) {
        source[name] = value.toString()
    }

    override fun callFunction(source: MutableMap<String, String>, name: String, type: Type, values: Array<Any?>): Any? = TODO()
}
