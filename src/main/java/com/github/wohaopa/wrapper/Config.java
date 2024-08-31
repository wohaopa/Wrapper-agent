package com.github.wohaopa.wrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Config {

    private static final Config instance = new Config();

    private List<String> extraModsDirs = null;
    private String mainModsDir = "mods";
    private String mainModsDirWithSeq = "mods/";
    private String configDIr = "config";
    private String modsListFile = null;

    public static _Config config;

    private Map<String, Set<String>> needTransform = null;

    public static String getModListFile() {
        return instance.modsListFile;
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

    private static void setModsListFile(String modsListFile) {
        if (modsListFile == null || modsListFile.isEmpty()) instance.modsListFile = null;
        else instance.modsListFile = modsListFile;
    }

    private static void setNeedTransform(Map<String, Set<String>> map) {
        if (map == null || map.isEmpty()) instance.needTransform = null;
        else instance.needTransform = map;
    }

    public static void loadConfig() {
        File configFile = new File("wrapper_config.json").getAbsoluteFile();
        if (!configFile.exists()) {
            try {
                exportFileFromJar(configFile, "settings/wrapper_config.json");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (configFile.exists()) try {
            readConfigInternal(configFile);
            setConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void readConfigInternal(File file) throws IOException {
        String context = readFileInternal(file);
        Gson gson = new Gson();

        try {
            config = gson.fromJson(context, _Config.class);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException(e);
        }

        setNeedTransform(config.needTransform_mods);
    }

    public static void setConfig() {
        if (config.settings.containsKey(config.active)) {
            _ConfigItem configItem = config.settings.get(config.active);
            setConfigDIr(configItem.config);
            setMainModsDir(configItem.main_mods);
            setExtraModsDirs(configItem.extra_mods);
            setModsListFile(configItem.modsListFile);
        } else {
            WrapperLog.log.warning("active: " + config.active + " is null!");
            setConfigDIr(null);
            setMainModsDir(null);
            setExtraModsDirs(null);
            setModsListFile(null);
        }
    }

    private static String readFileInternal(File file) throws IOException {
        return Files.asCharSource(file, Charsets.UTF_8)
            .read()
            .replace("\r\n", "\n");
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

    public static class _Config {

        public Map<String, Set<String>> needTransform_mods;
        public Map<String, Set<String>> needTransform_extraMods;
        public String active;
        public Map<String, _ConfigItem> settings;
    }

    public static class _ConfigItem {

        public String config;
        public String main_mods;
        public List<String> extra_mods;
        public String modsListFile;
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
}
