package com.github.wohaopa.wrapper.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;

import com.github.wohaopa.wrapper.Config;
import com.github.wohaopa.wrapper.Utility;

public class ModsDirFixTransformer implements ClassFileTransformer {

    private static final Map<String, Set<String>> transformedClassesMap = Config.getNeedTransform();
    private static final Set<String> transformedClasses = transformedClassesMap == null ? new HashSet<>()
        : transformedClassesMap.keySet();

    private static final boolean DEBUG = System.getProperty("ZPW_DEBUG", "false")
        .equals("true");

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (transformedClassesMap == null) return null;

        if (DEBUG || transformedClasses.contains(className)) {
            byte[] bytes = transformClass(classfileBuffer, className);
            if (DEBUG && ModifyMethodVisitor.needSave.contains(className)) {
                ModifyMethodVisitor.needSave.remove(className);
                Utility.saveClass(className, bytes);
            }
            return bytes;
        }
        return null;
    }

    public static byte[] transformClass(byte[] classfileBuffer, String className) {
        Set<String> needTransform = transformedClassesMap.get(className);

        ClassReader classReader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                if (DEBUG || needTransform.contains(name)) return new ModifyMethodVisitor(mv, className, name);
                return mv;
            }
        };

        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    private static class ModifyMethodVisitor extends InstructionAdapter {

        static Set<String> needSave = new ConcurrentSkipListSet<>();
        private final String className;
        private final String methodName;

        public ModifyMethodVisitor(MethodVisitor mv, String className, String methodName) {
            super(Opcodes.ASM5, mv);
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value.equals("mods/") || value.equals("mods")) {
                needSave.add(className);
                if (!DEBUG || transformedClasses.contains(className)) {
                    System.out.println("[Wrapper]: transformed " + className + ";" + methodName);
                    super.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/github/wohaopa/wrapper/Config",
                        "getMainModsDir",
                        "()Ljava/lang/String;",
                        false);
                    return;
                } else {
                    System.out.println("[Wrapper]: untransformed " + className + ";" + methodName);
                }
            }
            super.visitLdcInsn(value);
        }
    }
}
