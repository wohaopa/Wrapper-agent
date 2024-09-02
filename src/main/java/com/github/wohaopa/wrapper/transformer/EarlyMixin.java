package com.github.wohaopa.wrapper.transformer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EarlyMixin implements IMethodAdapter {

    public static EarlyMixin instance = new EarlyMixin();

    private EarlyMixin() {}

    @Override
    public MethodVisitor getMethodVisitor(MethodVisitor mv) {
        return new MethodVisitor(Opcodes.ASM5, mv) {

            @Override
            public void visitCode() {
                super.visitCode();

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
}
