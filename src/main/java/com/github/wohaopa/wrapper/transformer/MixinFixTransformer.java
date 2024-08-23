package com.github.wohaopa.wrapper.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.github.wohaopa.wrapper.Utility;

public class MixinFixTransformer implements ClassFileTransformer {

    private static final Set<String> transformedClasses = Utility.createImmutableSet(
        "com/gtnewhorizon/gtnhmixins/MinecraftURLClassPath",
        "ru/timeconqueror/spongemixins/repackage/com/gtnewhorizon/gtnhmixins/MinecraftURLClassPath",
        "com/gtnewhorizons/modularui/mixinplugin/MixinPlugin",
        "com/kuba6000/mobsinfo/mixin/MixinPlugin",
        "com/sinthoras/hydroenergy/mixinplugin/MixinPlugin",
        "kubatech/mixin/MixinPlugin");

    private static final boolean DEBUG = System.getProperty("ZPW_DEBUG_MixinFixTransformer", "false")
        .equals("true");

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (transformedClasses.contains(className)) {
            byte[] bytes = transformClass(classfileBuffer);
            if (DEBUG) Utility.saveClass(className, bytes);

            return bytes;
        }
        return null;
    }

    public static byte[] transformClass(byte[] classfileBuffer) {
        ClassReader classReader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                if ("findJarOf".contains(name) || "getJarInModPath".equals(name)) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {

                        @Override
                        public void visitCode() {
                            // 清除原来的方法体
                            super.visitCode();
                            // 插入对 newMethod 的静态调用
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "com/github/wohaopa/wrapper/Redirector",
                                "findJarOf",
                                "(Ljava/lang/Object;)Ljava/io/File;",
                                false);
                            mv.visitInsn(Opcodes.ARETURN);
                        }
                    };
                }
                return mv;
            }
        };

        classReader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }

}
