package com.github.wohaopa.wrapper.transformer;

import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.github.wohaopa.wrapper.Config;

public class MethodModify extends ASMModifyClass {

    public static MethodModify instance = new MethodModify();

    private MethodModify() {}

    private Map<String, Map<String, Set<IMethodAdapter>>> needTransformMethodNames = Config.needTransformMethodNames;

    @Override
    protected ClassVisitor getClassVisitor(ClassWriter classWriter, String className) {
        Map<String, Set<IMethodAdapter>> methods = needTransformMethodNames.get(className);
        if (methods == null) throw new RuntimeException("Class " + className + " no Transform for method found");
        return new ClassVisitor(Opcodes.ASM5, classWriter) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                if (methods.containsKey(name)) for (IMethodAdapter m : methods.get(name)) mv = m.getMethodVisitor(mv);

                return mv;
            }
        };
    }

}
