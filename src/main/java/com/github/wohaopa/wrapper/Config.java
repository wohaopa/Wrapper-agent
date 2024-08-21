package com.github.wohaopa.wrapper;

import java.util.List;

public class Config {

    private static List<String> extraModsDirs = null;
    private static String mainModsDir = "mods";
    private static String configDIr = "config";

    public static String getModListFile() {
        return null;
    }

    public static String getMainModsDir() {
        return mainModsDir;
    }

    public static List<String> getExtraModsDirs() {
        return extraModsDirs;
    }

    public static String getConfigDir() {
        return configDIr;
    }

    public static void setExtraModsDirs(List<String> extraModsDirs) {
        if (extraModsDirs == null || extraModsDirs.isEmpty()) Config.extraModsDirs = null;
        else Config.extraModsDirs = extraModsDirs;
    }

    public static void setMainModsDir(String mainModsDir) {
        if (mainModsDir == null || mainModsDir.isEmpty()) Config.mainModsDir = "mods";
        else Config.mainModsDir = mainModsDir;
    }

    public static void setConfigDIr(String configDIr) {
        if (configDIr == null || configDIr.isEmpty()) Config.configDIr = "config";
        else Config.configDIr = configDIr;
    }

}
