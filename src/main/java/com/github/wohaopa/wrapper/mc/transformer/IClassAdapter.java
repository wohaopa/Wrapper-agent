package com.github.wohaopa.wrapper.mc.transformer;

public interface IClassAdapter {

    byte[] getBytecode(String className, byte[] classfileBuffer);
}
