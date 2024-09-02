package com.github.wohaopa.wrapper.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public abstract class ASMModifyClass implements IClassAdapter {

    @Override
    public byte[] getBytecode(String className, byte[] classfileBuffer) {

        ClassReader classReader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        ClassVisitor classVisitor = getClassVisitor(classWriter, className);

        classReader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }

    protected abstract ClassVisitor getClassVisitor(ClassWriter classWriter, String classname);

}
