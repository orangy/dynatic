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
        println("Call: $name with ${values.toList()}")
        return 1L
    }
}

interface IFace {
    val int: Int
}

interface DynamicInterface {
    var str: String
    val int: Int
    val iface: IFace

    fun callme(str: String, int: Int) : Long
}

fun use() {
    val instance = implementDynamic<DynamicInterface, Record>(RecordAccessor)(Record())
    //val instance = DynamicInterface_delegate_DelegateType(Record(), RecordAccessor)
    println(instance.str)
    println(instance.int)
    instance.str = "poor"
    println(instance.str)
    println(instance.iface.int)

    println(instance.callme("paramstr", 33))
}

class DynamicInterface_delegate_DelegateType(private val source: Record, private val accessor: DynamicAccessor<Record>) : DynamicInterface {
    override fun callme(str: String, int: Int): Long {
        return accessor.callFunction(source, "callme", Long::class.java, arrayOf(str, int)) as Long
    }

    override val iface: IFace
        get() = accessor.getProperty(source, "iface", IFace::class.java) as IFace

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
        val classReader = ClassReader("org.jetbrains.dynatic.DynamicInterface_delegate_DelegateType")
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

