package com.github.wohaopa.wrapper.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.wohaopa.wrapper.Config;
import com.github.wohaopa.wrapper.Utility;
import com.github.wohaopa.wrapper.WrapperLog;

public class WrapperTransformer implements ClassFileTransformer {

    private static final Map<String, IClassAdapter> adapters = Config.adapters;
    private static final Set<String> classname = Config.adapters == null ? new HashSet<>() : Config.adapters.keySet();

    private static final boolean DEBUG = Config.DEBUG;

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (classname.contains(className)) {
            if (DEBUG) {
                Utility.saveClass(className, classfileBuffer);
                byte[] bytes = adapters.get(className)
                    .getBytecode(className, classfileBuffer);
                Utility.saveClass(className + "_transformed", bytes);
                WrapperLog.log.info("Saved class: " + className);
                return bytes;
            }
            return adapters.get(className)
                .getBytecode(className, classfileBuffer);
        }
        return null;
    }
}
