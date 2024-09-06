package com.github.wohaopa.wrapper.mc.transformer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DepLoader implements IMethodAdapter {

    public static DepLoader instance = new DepLoader();

    private DepLoader() {}

    @Override
    public MethodVisitor getMethodVisitor(MethodVisitor mv) {
        return new MethodVisitor(Opcodes.ASM5, mv) {

            @Override
            public void visitCode() {
                super.visitCode();

                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/github/wohaopa/wrapper/Redirector",
                    "modFiles",
                    "()Ljava/util/List;",
                    false);
                mv.visitInsn(Opcodes.ARETURN);
            }
        };
    }
}
