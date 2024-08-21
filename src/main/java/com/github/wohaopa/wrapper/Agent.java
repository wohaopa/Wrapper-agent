package com.github.wohaopa.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

public class Agent {

    public static Logger log = Logger.getLogger(Tags.Name);

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ReplaceClassFileTransformer());
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ReplaceClassFileTransformer());
    }
}

class ReplaceClassFileTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String classToReplace = "cpw/mods/fml/relauncher/CoreModManager";

        if (className.startsWith(classToReplace)) {
            Agent.log.info("Replacing class: " + className);
            return getReplacementClassBytes(className);
        }
        return null;
    }

    private byte[] getReplacementClassBytes(String className) {
        try (InputStream is = getClass().getResourceAsStream("/resources/" + className + ".class")) {
            if (is == null) {
                throw new IOException("Replacement class not found.");
            }
            return toByteArray(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
