package com.github.wohaopa.wrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.github.wohaopa.wrapper.transformer.DepLoader;
import com.github.wohaopa.wrapper.transformer.EarlyMixin;
import com.github.wohaopa.wrapper.transformer.IClassAdapter;
import com.github.wohaopa.wrapper.transformer.IMethodAdapter;
import com.github.wohaopa.wrapper.transformer.MethodModify;
import com.github.wohaopa.wrapper.transformer.ModsStringReplace;
import com.github.wohaopa.wrapper.transformer.ReplaceClass;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Config {

    public static boolean DEBUG = false;
    public static Map<String, IClassAdapter> adapters = new HashMap<>();
    public static Map<String, Map<String, Set<IMethodAdapter>>> needTransformMethodNames = new HashMap<>();
    public static Map<String, String> rename;

    private static final Config instance = new Config();

    private List<String> extraModsDirs = null;
    private String mainModsDir = "mods";
    private String mainModsDirWithSeq = "mods/";
    private String configDIr = "config";
    private String modsListFile = null;
    private String wrapperModsList = null;

    public static _Config config;

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

    public static String getWrapperModListFile() {
        return instance.wrapperModsList;
    }

    private static void setExtraModsDirs(List<String> extraModsDirs) {
        if (extraModsDirs == null || extraModsDirs.isEmpty()) instance.extraModsDirs = null;
        Iterator<String> iterator = extraModsDirs.iterator();
        while (iterator.hasNext()) {
            String dir = iterator.next();
            File file = new File(dir);
            if (!file.exists()) if (!file.mkdirs()) iterator.remove();
        }

        instance.extraModsDirs = extraModsDirs;
    }

    private static void setMainModsDir(String mainModsDir) {
        if (mainModsDir == null || mainModsDir.isEmpty() || !new File(mainModsDir).isDirectory()) {
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
        if (configDIr == null || configDIr.isEmpty() || !new File(configDIr).isDirectory())
            instance.configDIr = "config";
        else instance.configDIr = configDIr;
    }

    public static void setModsListFile(String modsListFile) {
        if (modsListFile == null || modsListFile.isEmpty() || !new File(modsListFile).isFile())
            instance.modsListFile = null;
        else instance.modsListFile = modsListFile;
    }

    public static void setWrapperModsList(String wrapperModsList) {
        if (wrapperModsList == null || wrapperModsList.isEmpty() || !new File(wrapperModsList).isFile())
            instance.wrapperModsList = null;
        else instance.wrapperModsList = wrapperModsList;
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
            setTransformInfo();
            setRename();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void setRename() {
        rename = config.rename == null ? new HashMap<>() : config.rename;
    }

    private static void addAll(Set<String> set, IClassAdapter adapter, IMethodAdapter methodAdapter) {
        for (String token : set) {
            String[] string = token.split(";", 2);
            adapters.put(string[0], adapter);
            if (string.length == 2 && methodAdapter != null) {
                Map<String, Set<IMethodAdapter>> methods = needTransformMethodNames
                    .computeIfAbsent(string[0], k -> new HashMap<>());
                methods.computeIfAbsent(string[1], k -> new HashSet<>())
                    .add(methodAdapter);
            }
        }
    }

    private static void setTransformInfo() {
        config.transform.forEach((s, stringSet) -> {
            switch (s) {
                case "ModsStringReplace": {
                    addAll(stringSet, MethodModify.instance, ModsStringReplace.instance);
                    break;
                }
                case "DepLoader": {
                    addAll(stringSet, MethodModify.instance, DepLoader.instance);
                    break;
                }
                case "EarlyMixin": {
                    addAll(stringSet, MethodModify.instance, EarlyMixin.instance);
                    break;
                }
                case "ReplaceClass": {
                    addAll(stringSet, ReplaceClass.instance, null);
                    break;
                }
                default:
            }
        });
    }

    private static void readConfigInternal(File file) throws IOException {
        String context = readFileInternal(file);
        Gson gson = new Gson();

        try {
            config = gson.fromJson(context, _Config.class);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setConfig() {
        if (config.settings.containsKey(config.active)) {
            _ConfigItem configItem = config.settings.get(config.active);
            setConfigDIr(configItem.config);
            setMainModsDir(configItem.main_mods);
            setExtraModsDirs(configItem.extra_mods);
            setModsListFile(configItem.modsListFile);
            setWrapperModsList(configItem.wrapperModsList);
        } else {
            WrapperLog.log.warning("active: " + config.active + " is null!");
            setConfigDIr(null);
            setMainModsDir(null);
            setExtraModsDirs(null);
            setModsListFile(null);
            setWrapperModsList(null);
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

        public Map<String, Set<String>> transform;
        public String active;
        public Map<String, _ConfigItem> settings;
        public Map<String, String> rename;
    }

    public static class _ConfigItem {

        public String config;
        public String main_mods;
        public List<String> extra_mods;
        public String modsListFile;
        public String wrapperModsList;

    }

}
