package com.github.wohaopa.wrapper.mc.transformer;

import com.github.wohaopa.wrapper.utils.Utility;
import com.github.wohaopa.wrapper.utils.WrapperLog;

public class ReplaceClass implements IClassAdapter {

    public static ReplaceClass instance = new ReplaceClass();

    private ReplaceClass() {}

    @Override
    public byte[] getBytecode(String className, byte[] classfileBuffer) {
        WrapperLog.log.info("Replacing class: " + className);
        return Utility.getReplacementClassBytes(className);
    }
}
