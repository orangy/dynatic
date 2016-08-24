package org.jetbrains.dynatic

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*

class DynamicGenerator(val generateKlass: String, val sourceKlass: String) {
    private val accessorKlass = Type.getInternalName(DynamicAccessor::class.java)

    private val classWriter = ClassWriter(0)

    fun begin(protoName: String) {
        classWriter.visit(V1_7, ACC_FINAL + ACC_SUPER, generateKlass, null, "java/lang/Object", arrayOf(protoName))
        classWriter.visitField(ACC_PRIVATE + ACC_FINAL, "source", "L$sourceKlass;", null, null).visitEnd()
        classWriter.visitField(ACC_PRIVATE + ACC_FINAL, "accessor", "L$accessorKlass;", "L$accessorKlass<L$sourceKlass;>;", null).visitEnd()

        constructor()
    }

    fun end() {
        classWriter.visitEnd()
    }

    fun getBytes(): ByteArray = classWriter.toByteArray()

    fun constructor() {
        classWriter.visitMethod(ACC_PUBLIC, "<init>", "(L$sourceKlass;L$accessorKlass;)V", "(L$sourceKlass;L$accessorKlass<-L$sourceKlass;>;)V", null).apply {
            visitCode()
            visitVarInsn(ALOAD, 1)
            visitLdcInsn("source")
            visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false)
            visitVarInsn(ALOAD, 2)
            visitLdcInsn("accessor")
            visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false)
            visitVarInsn(ALOAD, 0)
            visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            visitVarInsn(ALOAD, 0)
            visitVarInsn(ALOAD, 1)
            visitFieldInsn(PUTFIELD, generateKlass, "source", "L$sourceKlass;")
            visitVarInsn(ALOAD, 0)
            visitVarInsn(ALOAD, 2)
            visitFieldInsn(PUTFIELD, generateKlass, "accessor", "L$accessorKlass;")
            visitInsn(RETURN)
            visitMaxs(2, 3)
            visitEnd()
        }
    }

    fun function(name: String, type: Type, parameters: List<Type>) {
        val paramSignature = parameters.joinToString("", prefix = "(", postfix = ")")
        classWriter.visitMethod(ACC_PUBLIC, name, "${paramSignature}$type", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "accessor", "L$accessorKlass;")
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "source", "L$sourceKlass;")
            visitLdcInsn(name)
            visitLdcInsn(getBoxedType(type))

            visitLdcInsn(parameters.size)
            visitTypeInsn(ANEWARRAY, "java/lang/Object");
            parameters.forEachIndexed { index, type ->
                visitInsn(DUP);
                visitLdcInsn(index)
                boxTo(type, index + 1)
                visitInsn(AASTORE);
            }

            visitMethodInsn(INVOKEINTERFACE, accessorKlass, "callFunction", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/reflect/Type;[Ljava/lang/Object;)Ljava/lang/Object;", true)
            unboxReturn(type)
            visitMaxs(8, parameters.size + 2)
            visitEnd()
        }
    }

    fun getProperty(name: String, type: Type) {
        classWriter.visitMethod(ACC_PUBLIC, "get${name.capitalize()}", "()$type", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "accessor", "L$accessorKlass;")
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "source", "L$sourceKlass;")
            visitLdcInsn(name)
            visitLdcInsn(getBoxedType(type))
            visitMethodInsn(INVOKEINTERFACE, accessorKlass, "getProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/reflect/Type;)Ljava/lang/Object;", true)
            unboxReturn(type)
            visitMaxs(5, 1)
            visitEnd()
        }
    }

    fun setProperty(name: String, type: Type) {
        classWriter.visitMethod(ACC_PUBLIC, "set${name.capitalize()}", "($type)V", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "accessor", "L$accessorKlass;")
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "source", "L$sourceKlass;")
            visitLdcInsn(name)
            visitLdcInsn(getBoxedType(type))
            boxTo(type, 1)
            visitMethodInsn(INVOKEINTERFACE, accessorKlass, "setProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/reflect/Type;Ljava/lang/Object;)V", true)
            visitInsn(RETURN)
            visitMaxs(6, 3)
            visitEnd()
        }
    }

    private fun MethodVisitor.boxTo(type: Type, index: Int) {
        when (type) {
            Type.INT_TYPE -> {
                visitVarInsn(ILOAD, index)
                visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
            }
            Type.BOOLEAN_TYPE -> {
                visitVarInsn(ILOAD, index)
                visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
            }
            Type.LONG_TYPE -> {
                visitVarInsn(LLOAD, index)
                visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
            }
            Type.DOUBLE_TYPE -> {
                visitVarInsn(DLOAD, index)
                visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
            }
            else -> {
                visitVarInsn(ALOAD, index)
            }
        }
    }

    private fun MethodVisitor.unboxReturn(type: Type) {
        when (type) {
            Type.INT_TYPE -> {
                visitTypeInsn(CHECKCAST, "java/lang/Number")
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false)
                visitInsn(IRETURN)
            }
            Type.BOOLEAN_TYPE -> {
                visitTypeInsn(CHECKCAST, "java/lang/Boolean")
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false)
                visitInsn(IRETURN)
            }
            Type.LONG_TYPE -> {
                visitTypeInsn(CHECKCAST, "java/lang/Number")
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false)
                visitInsn(LRETURN)
            }
            Type.DOUBLE_TYPE -> {
                visitTypeInsn(CHECKCAST, "java/lang/Number")
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false)
                visitInsn(DRETURN)
            }
            Type.VOID_TYPE -> {
                visitInsn(RETURN)
            }
            else -> {
                visitTypeInsn(CHECKCAST, type.internalName)
                visitInsn(ARETURN)
            }
        }
    }

    private fun getBoxedType(klass: Type): Type =
            when (klass.sort) {
                Type.VOID -> Type.getObjectType("java/lang/Void")
                Type.BYTE -> Type.getObjectType("java/lang/Byte")
                Type.BOOLEAN -> Type.getObjectType("java/lang/Boolean")
                Type.SHORT -> Type.getObjectType("java/lang/Short")
                Type.CHAR -> Type.getObjectType("java/lang/Character")
                Type.INT -> Type.getObjectType("java/lang/Integer")
                Type.FLOAT -> Type.getObjectType("java/lang/Float")
                Type.LONG -> Type.getObjectType("java/lang/Long")
                Type.DOUBLE -> Type.getObjectType("java/lang/Double")
                else -> klass
            }
}