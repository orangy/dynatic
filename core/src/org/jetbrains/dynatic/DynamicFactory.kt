package org.jetbrains.dynatic

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*

internal fun generateFactory(classLoader: EmitClassLoader, generateKlassName: String, sourceKlass: String): Class<*> {
    val factoryKlassName = "$generateKlassName\$factory"
    val generateKlass = generateKlassName.replace('.', '/')
    val factoryKlass = factoryKlassName.replace('.', '/')
    val functionKlass = Type.getInternalName(Function1::class.java)
    val accessorKlass = Type.getInternalName(DynamicAccessor::class.java)
    val bytes = ClassWriter(0).apply {
        visit(V1_8, ACC_FINAL + ACC_SUPER + ACC_PUBLIC, factoryKlass,
                "L$functionKlass<L$sourceKlass;L$generateKlass;>;"
                , "java/lang/Object", arrayOf(functionKlass))

        visitField(ACC_PRIVATE + ACC_FINAL, "accessor", "L$accessorKlass;", "L$accessorKlass<L$sourceKlass;>;", null).visitEnd()

        visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitVarInsn(ALOAD, 1)
            visitTypeInsn(CHECKCAST, sourceKlass)
            visitMethodInsn(INVOKEVIRTUAL, factoryKlass, "invoke", "(L$sourceKlass;)L$generateKlass;", false)
            visitInsn(ARETURN)
            visitMaxs(3, 3)
            visitEnd()
        }

        visitMethod(ACC_PUBLIC + ACC_FINAL, "invoke", "(L$sourceKlass;)L$generateKlass;", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 1)
            visitLdcInsn("source")
            visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false)

            visitTypeInsn(NEW, generateKlass)
            visitInsn(DUP)
            visitVarInsn(ALOAD, 1)
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(GETFIELD, factoryKlass, "accessor", "L$accessorKlass;")
            visitMethodInsn(INVOKESPECIAL, generateKlass, "<init>", "(L$sourceKlass;L$accessorKlass;)V", false)
            visitInsn(ARETURN)
            visitMaxs(4, 3)
            visitEnd()
        }

        visitMethod(ACC_PUBLIC, "<init>", "(L$accessorKlass;)V", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            visitVarInsn(ALOAD, 0)
            visitVarInsn(ALOAD, 1)
            visitFieldInsn(PUTFIELD, factoryKlass, "accessor", "L$accessorKlass;")
            visitInsn(RETURN)
            visitMaxs(2, 2)
            visitEnd()
        }
        visitEnd()
    }.toByteArray()
    return classLoader.defineClass(factoryKlassName, bytes)
}