package org.jetbrains.dynatic.test

import org.jetbrains.dynatic.*
import org.junit.*
import java.lang.reflect.*
import kotlin.test.*

class FunctionsTests {
    @Test fun callFunctions() {
        val dynamic = implementDynamic<Functions, Register>(DummyAccessor)
        Register().apply {
            dynamic(this).empty()
            assertEquals("empty()", log)
        }
        Register().apply {
            dynamic(this).string("xxx")
            assertEquals("string(xxx)", log)
        }
        Register().apply {
            dynamic(this).int(22)
            assertEquals("int(22)", log)
        }
        Register().apply {
            val long = dynamic(this).getLong()
            assertEquals("getLong()", log)
            assertEquals(1, long)
        }
    }
}

interface Functions {
    fun empty()
    fun string(str: String)
    fun int(int: Int)
    fun getLong(): Long
}

class Register {
    var log: String = ""
}

object DummyAccessor : DynamicAccessor<Register> {
    override fun getProperty(source: Register, name: String, type: java.lang.reflect.Type): Any? = TODO()
    override fun setProperty(source: Register, name: String, type: Type, value: Any?) = TODO()
    override fun callFunction(source: Register, name: String, type: Type, values: Array<Any?>): Any? {
        source.log = "$name(${values.joinToString(",")})"
        return when (type) {
            Long::class.javaObjectType -> 1L
            else -> Unit
        }
    }
}
