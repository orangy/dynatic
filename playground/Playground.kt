package org.jetbrains.dynatic

import org.objectweb.asm.*
import org.objectweb.asm.util.*
import java.io.*
import java.lang.reflect.Type

class Record
object RecordAccessor : DynamicAccessor<Record> {
    var string = "ultimate"
    override fun getProperty(source: Record, name: String, type: Type): Any? {
        return when (name) {
            "int" -> 42
            "str" -> string
            "iface" -> implementDynamic((type as Class<*>).kotlin, Record::class, RecordAccessor)(source)
            else -> TODO()
        }
    }

    override fun setProperty(source: Record, name: String, type: Type, value: Any?) {
        if (name == "str")
            string = value as String
    }

    override fun callFunction(source: Record, name: String, type: Type, values: Array<Any?>): Any? {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

interface IFace {
    val int: Int
}

interface DynamicInterface {
    var str: String
    val int: Int
    val iface: IFace
}

fun use() {
    val factory = implementDynamic<DynamicInterface, Record>(RecordAccessor)
    val instance = factory(Record())
    println(instance.str)
    println(instance.int)
    instance.str = "poor"
    println(instance.str)
    println(instance.iface.int)
}

class DynamicInterface_delegate_DelegateType(private val source: Record, private val accessor: DynamicAccessor<Record>) : DynamicInterface {
    override val iface: IFace
        get() = throw UnsupportedOperationException()
    override var str: String
        get() = accessor.getProperty(source, "str", String::class.java) as String
        set(value) = accessor.setProperty(source, "str", String::class.java, value)

    override val int: Int
        get() = accessor.getProperty(source, "int", Int::class.java) as Int
}

fun main(args: Array<String>) {
    use()
    return
    run {
        val classReader = ClassReader("org.jetbrains.elements.dynamic.DynamicInterface_delegate_DelegateType")
        val writer = ClassWriter(0)
        val classVisitor = TraceClassVisitor(writer, ASMifier(), PrintWriter(System.out));
        classReader.accept(classVisitor, 0);
    }
/*
    run {
        val classReader = ClassReader("org.jetbrains.elements.dynamic.DynamicInterface_delegate_DelegateType\$str\$1")
        val writer = ClassWriter(0)
        val classVisitor = TraceClassVisitor(writer, ASMifier(), PrintWriter(System.out));
        classReader.accept(classVisitor, 0);
    }
*/
}

