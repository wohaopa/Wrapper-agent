package com.github.wohaopa.wrapper.transformer;

import com.github.wohaopa.wrapper.Utility;
import com.github.wohaopa.wrapper.WrapperLog;

public class ReplaceClass implements IClassAdapter {

    public static ReplaceClass instance = new ReplaceClass();

    private ReplaceClass() {}

    @Override
    public byte[] getBytecode(String className, byte[] classfileBuffer) {
        WrapperLog.log.info("Replacing class: " + className);
        return Utility.getReplacementClassBytes(className);
    }
}
