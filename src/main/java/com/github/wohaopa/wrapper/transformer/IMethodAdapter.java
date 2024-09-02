package com.github.wohaopa.wrapper.transformer;

import org.objectweb.asm.MethodVisitor;

public interface IMethodAdapter {

    MethodVisitor getMethodVisitor(MethodVisitor mv);
}
