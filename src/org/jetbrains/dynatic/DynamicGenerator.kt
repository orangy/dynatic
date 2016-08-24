package org.jetbrains.dynatic

import org.jetbrains.dynatic.*
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

    fun getProperty(propertyName: String, propertyType: Type) {
        classWriter.visitMethod(ACC_PUBLIC, "get${propertyName.capitalize()}", "()$propertyType", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "accessor", "L$accessorKlass;")
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "source", "L$sourceKlass;")
            visitLdcInsn(propertyName)
            visitLdcInsn(getBoxedType(propertyType))
            visitMethodInsn(INVOKEINTERFACE, accessorKlass, "getProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/reflect/Type;)Ljava/lang/Object;", true)
            when (propertyType) {
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
                else -> {
                    visitTypeInsn(CHECKCAST, propertyType.internalName)
                    visitInsn(ARETURN)
                }
            }
            visitMaxs(5, 1)
            visitEnd()
        }
    }

    fun setProperty(propertyName: String, propertyType: Type) {
        classWriter.visitMethod(ACC_PUBLIC, "set${propertyName.capitalize()}", "($propertyType)V", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "accessor", "L$accessorKlass;")
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, generateKlass, "source", "L$sourceKlass;")
            visitLdcInsn(propertyName)
            visitLdcInsn(getBoxedType(propertyType))
            when (propertyType) {
                Type.INT_TYPE -> {
                    visitVarInsn(Opcodes.ILOAD, 1)
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
                }
                Type.BOOLEAN_TYPE -> {
                    visitVarInsn(Opcodes.ILOAD, 1)
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
                }
                Type.LONG_TYPE -> {
                    visitVarInsn(Opcodes.LLOAD, 1)
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
                }
                Type.DOUBLE_TYPE -> {
                    visitVarInsn(Opcodes.DLOAD, 1)
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
                }
                else -> {
                    visitVarInsn(Opcodes.ALOAD, 1)
                }
            }
            visitMethodInsn(INVOKEINTERFACE, accessorKlass, "setProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/reflect/Type;Ljava/lang/Object;)V", true)
            visitInsn(RETURN)
            visitMaxs(6, 3)
            visitEnd()
        }
    }

    private fun getBoxedType(klass: Type): Type =
            when (klass.sort) {
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