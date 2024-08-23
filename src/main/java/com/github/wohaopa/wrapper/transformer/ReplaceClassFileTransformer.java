package com.github.wohaopa.wrapper.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;

import com.github.wohaopa.wrapper.Utility;
import com.github.wohaopa.wrapper.WrapperLog;

public class ReplaceClassFileTransformer implements ClassFileTransformer {

    private static final Set<String> replacements = Utility.createImmutableSet(
        "cpw/mods/fml/common/Loader",
        "cpw/mods/fml/common/Loader$ModIdComparator",
        "cpw/mods/fml/common/Loader$1",
        "cpw/mods/fml/common/Loader$2",
        "cpw/mods/fml/common/Loader$3",
        "cpw/mods/fml/relauncher/CoreModManager",
        "cpw/mods/fml/relauncher/CoreModManager$FMLPluginWrapper",
        "cpw/mods/fml/relauncher/CoreModManager$1",
        "cpw/mods/fml/relauncher/CoreModManager$2",
        "cpw/mods/fml/relauncher/CoreModManager$3",
        "cpw/mods/fml/relauncher/CoreModManager$4");

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        if (replacements.contains(className)) {
            WrapperLog.log.info("Replacing class: " + className);
            return Utility.getReplacementClassBytes(className);
        }
        return null;
    }

}
