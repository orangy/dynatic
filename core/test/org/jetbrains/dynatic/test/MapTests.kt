package org.jetbrains.dynatic.test

import org.jetbrains.dynatic.*
import org.junit.*
import java.lang.reflect.*
import kotlin.test.*

class MapTests {
    @Test fun interpretAsStrings() {
        val named = mapOf("name" to "value").mapCast<Strings>()
        assertEquals("value", named.name)
    }

    @Test fun interpretAsNumbers() {

        val numbers = mapOf(
                "count" to 12,
                "size" to 42L,
                "percent" to 99.9).mapCast<Numbers>()
        assertEquals(12, numbers.count)
        assertEquals(42L, numbers.size)
        assertEquals(99.9, numbers.percent)
    }

    @Test fun interpretAsCascade() {
        val cascade = mapOf(
                "numbers" to mapOf("count" to 1, "size" to 123L),
                "dates" to mapOf("created" to "2014-12-1")
        ).mapCast<Cascade>()
        assertEquals(1, cascade.numbers.count)
        assertEquals(123L, cascade.numbers.size)
        assertEquals("2014-12-1", cascade.dates.created)
    }
}

inline fun <reified V : Any> Map<String, Any?>.mapCast(): V = implementDynamic<V, Map<String, Any?>>(MapAccessor)(this)

object MapAccessor : DynamicAccessor<Map<String, Any?>> {
    override fun getProperty(source: Map<String, Any?>, name: String, type: Type): Any? {
        val value = source[name] ?: throw IllegalAccessException("property $name not found")
        return if (value is Map<*, *>) {
            implementDynamic((type as Class<*>).kotlin, Map::class, MapAccessor)(value)
        } else value
    }

    override fun setProperty(source: Map<String, Any?>, name: String, type: Type, value: Any?) = TODO()
    override fun callFunction(source: Map<String, Any?>, name: String, type: Type, values: Array<Any?>): Any? = TODO()
}
