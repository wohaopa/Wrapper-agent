package com.github.wohaopa.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.wohaopa.wrapper.mc.transformer.DepLoader;
import com.github.wohaopa.wrapper.mc.transformer.EarlyMixin;
import com.github.wohaopa.wrapper.mc.transformer.IClassAdapter;
import com.github.wohaopa.wrapper.mc.transformer.IMethodAdapter;
import com.github.wohaopa.wrapper.mc.transformer.MethodModify;
import com.github.wohaopa.wrapper.mc.transformer.ModsStringReplace;
import com.github.wohaopa.wrapper.mc.transformer.ReplaceClass;
import com.github.wohaopa.wrapper.utils.JSONUtility;
import com.github.wohaopa.wrapper.utils.Utility;
import com.github.wohaopa.wrapper.utils.WrapperLog;

public class Config {

    // Wrapper自身配置，不可被用户更改
    public static boolean DEBUG = false;
    public static Map<String, IClassAdapter> adapters = new HashMap<>();
    public static Map<String, Map<String, Set<IMethodAdapter>>> needTransformMethodNames = new HashMap<>();
    public static Map<String, String> rename;

    private static _ConfigItem active;

    private static _Config config;

    // 获得配置信息
    public static String getMainModsDir() {
        return active.main_mods;
    }

    public static String getMainModsDirWithSep() {
        return active.main_mods + File.separator;
    }

    public static List<String> getExtraModsDirs() {
        return active.extra_mods;
    }

    public static String getConfigDir() {
        return active.config;
    }

    public static String getWrapperModListFile() {
        return active.wrapperModsList;
    }

    public static String getModListFile() {
        return active.modsListFile;
    }

    public static Set<String> getAllSettings() {
        return config.settings.keySet();
    }

    // 设置配置信息

    public static void setConfigDIr(_ConfigItem item, String configDIr) {
        if (!Utility.parseDir(configDIr)) item.config = "config";
        else item.config = configDIr;
    }

    public static void setMainModsDir(_ConfigItem item, String mainModsDir) {
        if (!Utility.parseDir(mainModsDir)) {
            item.main_mods = "mods";
        } else if (mainModsDir.endsWith("/")) {
            int index = mainModsDir.length();
            while (index > 0 && mainModsDir.charAt(index - 1) == '/') {
                index--;
            }

            item.main_mods = mainModsDir.substring(0, index + 1);
        } else {
            item.main_mods = mainModsDir;
        }
    }

    public static void setExtraModsDirs(_ConfigItem item, List<String> extraModsDirs) {
        if (extraModsDirs == null || extraModsDirs.isEmpty()) {
            item.extra_mods = null;
            return;
        }
        extraModsDirs.removeIf(dir -> !Utility.parseDir(dir));

        item.extra_mods = extraModsDirs;
    }

    public static void setModsListFile(String modsListFile) {
        setModsListFile(active, modsListFile);
    }

    public static void setModsListFile(_ConfigItem item, String modsListFile) {
        if (modsListFile == null || modsListFile.isEmpty() || !new File(modsListFile).isFile())
            item.modsListFile = null;
        else item.modsListFile = modsListFile;
    }

    public static void setWrapperModsList(String wrapperModsList) {
        setWrapperModsList(active, wrapperModsList);
    }

    public static void setWrapperModsList(_ConfigItem item, String wrapperModsList) {
        if (wrapperModsList == null || wrapperModsList.isEmpty() || !new File(wrapperModsList).isFile())
            item.wrapperModsList = null;
        else item.wrapperModsList = wrapperModsList;
    }

    public static void loadConfig() {
        File configFile = new File("wrapper_config.json").getAbsoluteFile();
        if (!configFile.exists()) {
            try {
                Utility.exportFileFromJar(configFile, "settings/wrapper_config.json");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (configFile.exists()) {
            config = JSONUtility.loadFromFile(configFile, _Config.class);
            if (config == null) {
                throw new RuntimeException("Config file does not exist: " + configFile);
            }
            setConfig(config.active);
            setTransformInfo();
            setRename();
        }

    }

    public static void setConfig(String activeSetting) {
        config.active = activeSetting;
        if (config.settings.containsKey(activeSetting)) {
            active = config.settings.get(activeSetting);
            setConfigDIr(active, active.config);
            setMainModsDir(active, active.main_mods);
            setExtraModsDirs(active, active.extra_mods);
            setModsListFile(active, active.modsListFile);
            setWrapperModsList(active, active.wrapperModsList);
        } else {
            WrapperLog.log.warning("active: " + activeSetting + " is null!");
            active = new _ConfigItem();
            config.settings.put(activeSetting, active);
            setConfigDIr(active, null);
            setMainModsDir(active, null);
            setExtraModsDirs(active, null);
            setModsListFile(active, null);
            setWrapperModsList(active, null);
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

    public static String getActiveSetting() {
        return config.active;
    }

    public static _ConfigItem getSetting(String setting) {
        return config.settings.get(setting);
    }

    public static _ConfigItem renameSetting(String newValue, String oldValue) {
        _ConfigItem item = config.settings.remove(oldValue);
        config.settings.put(newValue, item);
        return item;
    }

    public static _ConfigItem newSetting(String setting) {
        _ConfigItem item = new _ConfigItem();
        setConfigDIr(item, null);
        setMainModsDir(item, null);
        setExtraModsDirs(item, null);
        setModsListFile(item, null);
        setWrapperModsList(item, null);
        config.settings.put(setting, item);
        return item;
    }

    public static void saveConfig() {

        File configFile = new File("wrapper_config.json").getAbsoluteFile();
        JSONUtility.saveToFile(config, configFile, _Config.class);
    }

    public static void removeSetting(String setting) {
        config.settings.remove(setting);
    }

    public static class _Config {

        public String active;
        public Map<String, _ConfigItem> settings;
        public Map<String, String> rename; // 不可在编辑器内编辑
        public Map<String, Set<String>> transform; // 不可在编辑器内编辑
    }

    public static class _ConfigItem {

        public String config;
        public String main_mods;
        public List<String> extra_mods;
        public String modsListFile;
        public String wrapperModsList;
    }

}
