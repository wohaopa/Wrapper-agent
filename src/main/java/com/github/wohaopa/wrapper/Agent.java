package com.github.wohaopa.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Agent {

    public static void main(String[] args) {}

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ReplaceClassFileTransformer());
        loadConfig();
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ReplaceClassFileTransformer());
        loadConfig();
    }

    private static void loadConfig() {
        File modsDirs = new File("mods_dirs.txt").getAbsoluteFile();
        File configDir = new File("config_dir.txt").getAbsoluteFile();
        if (!modsDirs.isFile() && !configDir.isFile()) {
            File jsonConfig = new File("wrapper.json").getAbsoluteFile();
            if (jsonConfig.isFile()) {
                String json = null;
                try {
                    json = Files.asCharSource(jsonConfig, Charsets.UTF_8)
                        .read();
                } catch (IOException e1) {
                    WrapperLog.log.warning("Failed to load config file: " + jsonConfig);
                }
                if (json != null) {
                    Gson gsonParser = new Gson();

                    try {
                        ConfigJson jsonObject = gsonParser.fromJson(json, ConfigJson.class);
                        if (jsonObject != null && !jsonObject.active.isEmpty()) {
                            JsonObject setting = jsonObject.settings.getAsJsonObject(jsonObject.active);
                            if (setting != null) {
                                Iterator<JsonElement> iterator = setting.getAsJsonArray("modsDirs")
                                    .iterator();
                                String mainMods = null;
                                List<String> extraMods = new ArrayList<>();
                                while (iterator.hasNext()) {
                                    String modsDir = iterator.next()
                                        .getAsString();
                                    try {
                                        new File(modsDir).getCanonicalFile();
                                    } catch (IOException e) {
                                        WrapperLog.log
                                            .warning("This name does not conform to the folder name rules: " + modsDir);
                                        continue;
                                    }
                                    if (mainMods == null) mainMods = modsDir;
                                    else extraMods.add(modsDir);
                                }
                                Config.setMainModsDir(mainMods);
                                Config.setExtraModsDirs(extraMods);

                                String configDir1 = setting.get("configDir")
                                    .getAsString();
                                try {
                                    new File(configDir1).getCanonicalFile();
                                    Config.setConfigDIr(configDir1);
                                } catch (IOException e) {
                                    WrapperLog.log
                                        .warning("This name does not conform to the folder name rules: " + configDir1);
                                }
                            }
                        }
                    } catch (RuntimeException e) {
                        WrapperLog.log.warning("Failed to parse modList json file " + jsonConfig);
                    }
                }

            } else {
                modsDirs.delete();
                configDir.delete();
                jsonConfig.delete();
                File destDir = configDir.getParentFile();

                String jarFilePath = null;
                try {
                    jarFilePath = new File(
                        URLDecoder.decode(
                            Agent.class.getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .getPath(),
                            StandardCharsets.UTF_8.toString())).getAbsolutePath();
                } catch (UnsupportedEncodingException e) {
                    WrapperLog.log.warning("Path conversion failed: " + jarFilePath);
                }
                if (jarFilePath != null) {
                    try (JarFile jarFile = new JarFile(jarFilePath)) {
                        Enumeration<JarEntry> entries = jarFile.entries();

                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String entryName = entry.getName();

                            if (entryName.startsWith("settings") && !entry.isDirectory()) {
                                File destFile = new File(destDir, entryName.substring("settings".length() + 1));

                                try (InputStream inputStream = jarFile.getInputStream(entry);
                                    FileOutputStream outputStream = new FileOutputStream(destFile)) {

                                    byte[] buffer = new byte[1024];
                                    int bytesRead;
                                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, bytesRead);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        WrapperLog.log.warning("Failed to parse jar file " + e);
                    }
                }
            }
        } else {
            if (modsDirs.isFile()) {
                String context = null;
                try {
                    context = Files.asCharSource(modsDirs, Charsets.UTF_8)
                        .read();
                } catch (IOException e1) {
                    WrapperLog.log.warning("Failed to load config file: " + e1.getMessage());
                }
                if (context != null) {
                    String[] lines = context.split("\n");
                    String mainMods = null;
                    List<String> extraMods = new ArrayList<>();
                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            try {
                                new File(line).getCanonicalFile();
                            } catch (IOException e) {
                                WrapperLog.log.warning("This name does not conform to the folder name rules: " + line);
                                continue;
                            }
                            if (mainMods == null) mainMods = line;
                            else extraMods.add(line);
                        }
                    }
                    Config.setMainModsDir(mainMods);
                    Config.setExtraModsDirs(extraMods);
                }
            }
            if (configDir.isFile()) {
                String context = null;
                try {
                    context = Files.asCharSource(configDir, Charsets.UTF_8)
                        .read();
                } catch (IOException e1) {
                    WrapperLog.log.warning("Failed to load config file: " + e1.getMessage());
                }
                if (context != null) {
                    String[] lines = context.split("\n");
                    String config = null;
                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            try {
                                new File(line).getCanonicalFile();
                            } catch (IOException e) {
                                WrapperLog.log.warning("This name does not conform to the folder name rules: " + line);
                                continue;
                            }
                            config = line;
                            break;
                        }
                    }
                    Config.setConfigDIr(config);
                }
            }
        }
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
        String classToReplace = "cpw/mods/fml/relauncher/CoreModManager";

        if (className.startsWith(classToReplace)) {
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
