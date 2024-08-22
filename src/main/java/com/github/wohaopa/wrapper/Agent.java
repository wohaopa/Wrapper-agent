package com.github.wohaopa.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import com.google.gson.JsonObject;

public class Agent {

    public static void main(String[] args) {}

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ReplaceClassFileTransformer());
        if (agentArgs != null && agentArgs.equals("debug")) {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Config.loadConfig();
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ReplaceClassFileTransformer());
        Config.loadConfig();
    }

    public static class ConfigJson {

        public String active;
        public JsonObject settings;
    }
}

class ReplaceClassFileTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String coreModManagerClass = "cpw/mods/fml/relauncher/CoreModManager";
        String LoaderClass = "cpw/mods/fml/common/Loader";
        String LoaderClassC = "cpw/mods/fml/common/Loader$";

        if (className.startsWith(coreModManagerClass) || className.startsWith(LoaderClassC)
            || className.equals(LoaderClass)) {
            WrapperLog.log.info("Replacing class: " + className);
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
