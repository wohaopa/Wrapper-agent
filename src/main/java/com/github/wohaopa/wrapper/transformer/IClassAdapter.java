package com.github.wohaopa.wrapper.transformer;

public interface IClassAdapter {

    byte[] getBytecode(String className, byte[] classfileBuffer);
}
