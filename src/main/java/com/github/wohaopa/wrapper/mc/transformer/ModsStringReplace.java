package com.github.wohaopa.wrapper.mc.transformer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ModsStringReplace implements IMethodAdapter {

    public static ModsStringReplace instance = new ModsStringReplace();

    private ModsStringReplace() {}

    @Override
    public MethodVisitor getMethodVisitor(MethodVisitor mv) {
        return new MethodVisitor(Opcodes.ASM5, mv) {

            @Override
            public void visitLdcInsn(Object value) {
                if (value.equals("mods/")) {
                    super.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/github/wohaopa/wrapper/Config",
                        "getMainModsDirWithSep",
                        "()Ljava/lang/String;",
                        false);
                } else if (value.equals("mods")) {
                    super.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/github/wohaopa/wrapper/Config",
                        "getMainModsDir",
                        "()Ljava/lang/String;",
                        false);
                } else super.visitLdcInsn(value);
            }
        };
    }
}
