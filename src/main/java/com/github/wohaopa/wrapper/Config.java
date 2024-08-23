package com.github.wohaopa.wrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Config {

    private static final Config instance = new Config();

    private List<String> extraModsDirs = null;
    private String mainModsDir = "mods";
    private String mainModsDirWithSeq = "mods/";
    private String configDIr = "config";

    private Map<String, Set<String>> needTransform = null;

    public static String getModListFile() {
        return null;
    }

    public static String getMainModsDir() {
        return instance.mainModsDir;
    }

    public static String getMainModsDirWithSep() {
        return instance.mainModsDirWithSeq;
    }

    public static List<String> getExtraModsDirs() {
        return instance.extraModsDirs;
    }

    public static String getConfigDir() {
        return instance.configDIr;
    }

    public static Map<String, Set<String>> getNeedTransform() {
        return instance.needTransform;
    }

    private static void setExtraModsDirs(List<String> extraModsDirs) {
        if (extraModsDirs == null || extraModsDirs.isEmpty()) instance.extraModsDirs = null;
        else instance.extraModsDirs = extraModsDirs;
    }

    private static void setMainModsDir(String mainModsDir) {
        if (mainModsDir == null || mainModsDir.isEmpty()) {
            instance.mainModsDir = "mods";
            instance.mainModsDirWithSeq = "mods" + File.separator;
        } else if (mainModsDir.endsWith("/")) {
            int index = mainModsDir.length();
            while (index > 0 && mainModsDir.charAt(index - 1) == '/') {
                index--;
            }

            instance.mainModsDir = mainModsDir.substring(0, index + 1);
            instance.mainModsDirWithSeq = instance.mainModsDir + File.separator;
        } else {
            instance.mainModsDir = mainModsDir;
            instance.mainModsDirWithSeq = mainModsDir + File.separator;
        }
    }

    private static void setConfigDIr(String configDIr) {
        if (configDIr == null || configDIr.isEmpty()) instance.configDIr = "config";
        else instance.configDIr = configDIr;
    }

    private static void setNeedTransform(Map<String, Set<String>> map) {
        if (map == null || map.isEmpty()) instance.needTransform = null;
        else instance.needTransform = map;
    }

    public static void loadConfig() {
        File transformConfig = new File("WrapperNeedTransform.txt").getAbsoluteFile();
        if (!transformConfig.exists()) {
            try {
                exportFileFromJar(transformConfig, "settings/WrapperNeedTransform.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (transformConfig.exists()) readTransformConfigInternal(transformConfig);

        File modsDirs = new File("mods_dirs.txt").getAbsoluteFile();
        File configDir = new File("config_dir.txt").getAbsoluteFile();
        File jsonConfig = new File("wrapper.json").getAbsoluteFile();

        if (!modsDirs.exists() && !configDir.exists() && !jsonConfig.exists()) {
            try {
                exportFileFromJar(modsDirs, "settings/mods_dirs.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                exportFileFromJar(configDir, "settings/config_dir.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                exportFileFromJar(jsonConfig, "settings/wrapper.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (modsDirs.isFile()) {
            try {
                String context = readFile(modsDirs);
                String[] lines = context.split("\n");
                String mainMods = null;
                List<String> extraMods = new ArrayList<>();
                for (String line : lines) {
                    if (!line.isEmpty() && identifyFile(line)) {
                        if (mainMods == null) mainMods = line;
                        else extraMods.add(line);
                    }
                }
                Config.setMainModsDir(mainMods);
                Config.setExtraModsDirs(extraMods);

            } catch (IOException e) {
                WrapperLog.log.warning("Failed to load config file: " + e.getMessage());
            }
        }

        if (configDir.isFile()) {
            try {
                String context = readFile(configDir);
                String[] lines = context.split("\n");
                String config = null;
                for (String line : lines) {
                    if (!line.isEmpty() && identifyFile(line)) {
                        config = line;
                        break;
                    }
                }
                Config.setConfigDIr(config);
            } catch (IOException e1) {
                WrapperLog.log.warning("Failed to load config file: " + e1.getMessage());
            }
        }

        if (!modsDirs.exists() && !configDir.exists() && jsonConfig.exists()) {
            try {
                String json = readFile(jsonConfig);
                Gson gsonParser = new Gson();
                try {
                    Config.ConfigJson jsonObject = gsonParser.fromJson(json, Config.ConfigJson.class);
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
                                if (identifyFile(modsDir)) {
                                    if (mainMods == null) mainMods = modsDir;
                                    else extraMods.add(modsDir);
                                }
                            }
                            Config.setMainModsDir(mainMods);
                            Config.setExtraModsDirs(extraMods);

                            String configDir1 = setting.get("configDir")
                                .getAsString();
                            if (identifyFile(configDir1)) {
                                Config.setConfigDIr(configDir1);
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    WrapperLog.log.warning("Failed to parse modList json file " + jsonConfig);
                }
            } catch (IOException e1) {
                WrapperLog.log.warning("Failed to load config file: " + jsonConfig);
            }
        }

    }

    private static void readTransformConfigInternal(File transformConfig) {
        try {
            String context = readFile(transformConfig);
            String[] lines = context.split("\n");

            Map<String, Set<String>> map = new HashMap<>();
            for (String line : lines) {
                if (!line.isEmpty()) {
                    String[] items = line.split(";", 2);
                    String[] methods = items[1].split(",");
                    if (items.length == 2 && !items[0].isEmpty() && !items[1].isEmpty()) {
                        Set<String> set = new HashSet<>();
                        for (String method : methods) {
                            if (!method.isEmpty()) set.add(method);
                        }
                        map.put(items[0], set);
                    }
                }
            }

            Config.setNeedTransform(map);
        } catch (IOException e1) {
            WrapperLog.log.warning("Failed to load config file: " + e1.getMessage());
        }
    }

    private static String readFile(File file) throws IOException {
        return Files.asCharSource(file, Charsets.UTF_8)
            .read()
            .replace("\r\n", "\n");
    }

    private static boolean identifyFile(String fileName) {
        try {
            new File(fileName).getAbsoluteFile()
                .getCanonicalFile();
            return true;
        } catch (IOException e) {
            WrapperLog.log.warning("This name does not conform to the folder name rules: " + fileName);
            e.printStackTrace();
            return false;
        }
    }

    private static String jarFilePath;

    private static void exportFileFromJar(File destFile, String name) throws IOException {
        if (jarFilePath == null || jarFilePath.isEmpty()) {
            jarFilePath = new File(
                URLDecoder.decode(
                    Agent.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath(),
                    StandardCharsets.UTF_8.toString())).getAbsolutePath();
        }

        JarFile jarFile = new JarFile(jarFilePath);
        JarEntry jarEntry = jarFile.getJarEntry(name);
        if (jarEntry == null) throw new FileNotFoundException(name);

        File dir = destFile.getParentFile();
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Unable to create directory: " + dir.getAbsolutePath());

        InputStream inputStream = jarFile.getInputStream(jarEntry);
        FileOutputStream outputStream = new FileOutputStream(destFile);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        outputStream.close();
        jarFile.close();
    }

    public static class ConfigJson {

        public String active;
        public JsonObject settings;
    }
}
