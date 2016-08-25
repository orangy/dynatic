package org.jetbrains.dynatic

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*

internal fun generateFactory(classLoader: EmitClassLoader, generateKlassName: String, sourceKlass: String): Class<*> {
    val factoryKlassName = "$generateKlassName\$factory"
    val generateKlass = generateKlassName.replace('.', '/')
    val factoryKlass = factoryKlassName.replace('.', '/')
    val functionKlass = Type.getInternalName(Function2::class.java)
    val accessorKlass = Type.getInternalName(DynamicAccessor::class.java)
    val bytes = ClassWriter(0).apply {
        visit(V1_8, ACC_FINAL + ACC_SUPER + ACC_PUBLIC, factoryKlass,
                "L$functionKlass<L$sourceKlass;L$accessorKlass<-L$sourceKlass;>;L$generateKlass;>;"
                , "java/lang/Object", arrayOf(functionKlass))

        visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "invoke", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitVarInsn(ALOAD, 1)
            visitTypeInsn(CHECKCAST, sourceKlass)
            visitVarInsn(ALOAD, 2)
            visitTypeInsn(CHECKCAST, accessorKlass)
            visitMethodInsn(INVOKEVIRTUAL, factoryKlass, "invoke", "(L$sourceKlass;L$accessorKlass;)L$generateKlass;", false)
            visitInsn(ARETURN)
            visitMaxs(3, 3)
            visitEnd()
        }

        visitMethod(ACC_PUBLIC + ACC_FINAL, "invoke", "(L$sourceKlass;L$accessorKlass;)L$generateKlass;", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 1)
            visitLdcInsn("source")
            visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false)
            visitVarInsn(ALOAD, 2)
            visitLdcInsn("accessor")
            visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false)

            visitTypeInsn(NEW, generateKlass)
            visitInsn(DUP)
            visitVarInsn(ALOAD, 1)
            visitVarInsn(ALOAD, 2)
            visitMethodInsn(INVOKESPECIAL, generateKlass, "<init>", "(L$sourceKlass;L$accessorKlass;)V", false)
            visitInsn(ARETURN)
            visitMaxs(4, 3)
            visitEnd()
        }

        visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            visitInsn(RETURN)
            visitMaxs(1, 1)
            visitEnd()
        }
        visitEnd()
    }.toByteArray()
    return classLoader.defineClass(factoryKlassName, bytes)
}