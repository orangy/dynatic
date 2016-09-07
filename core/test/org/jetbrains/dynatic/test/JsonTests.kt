package org.jetbrains.dynatic.test

import com.google.gson.*
import org.jetbrains.dynatic.*
import org.junit.*
import java.lang.reflect.*
import kotlin.test.*

class JsonTests {
    @Test fun interpretAsStrings() {
        val named = "{ name: \"value\"}".jsonCast<Strings>()
        assertEquals("value", named.name)
    }

    @Test fun interpretAsNumbers() {
        val numbers = "{ count: 12, size: 42, percent: 99.9}".jsonCast<Numbers>()
        assertEquals(12, numbers.count)
        assertEquals(42L, numbers.size)
        assertEquals(99.9, numbers.percent)
    }

    @Test fun interpretAsCascade() {
        val cascade = """
        {
          numbers: { count:1, size:123 },
          dates: { created: "2014-12-1" }
        }
        """.jsonCast<Cascade>()
        assertEquals(1, cascade.numbers.count)
        assertEquals(123L, cascade.numbers.size)
        assertEquals("2014-12-1", cascade.dates.created)
    }
}

val jsonParser = JsonParser()
inline fun <reified V : Any> String.jsonCast(): V = implementDynamic<V, JsonObject>(JsonAccessor)(jsonParser.parse(this) as JsonObject)

object JsonAccessor : DynamicAccessor<JsonObject> {
    override fun getProperty(source: JsonObject, name: String, type: Type): Any? {
        val element = source[name]
        return when (element) {
            is JsonObject -> implementDynamic((type as Class<*>).kotlin, JsonObject::class, JsonAccessor)(element)
            is JsonPrimitive -> when {
                element.isBoolean -> element.asBoolean
                element.isNumber -> {
                    when (type) {
                        Int::class.javaPrimitiveType, Int::class.javaObjectType -> element.asInt
                        Long::class.javaPrimitiveType, Long::class.javaObjectType -> element.asLong
                        Double::class.javaPrimitiveType, Double::class.javaObjectType -> element.asDouble
                        else -> throw IllegalAccessException("property $name has unsupported type")
                    }
                }
                element.isString -> element.asString
                else -> throw IllegalAccessException("property $name has unsupported type")
            }
            else -> throw IllegalAccessException("property $name has unsupported type")
        }
    }

    override fun setProperty(source: JsonObject, name: String, type: Type, value: Any?) = TODO()
    override fun callFunction(source: JsonObject, name: String, type: Type, values: Array<Any?>): Any? = TODO()

}
