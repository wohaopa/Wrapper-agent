package com.github.wohaopa.wrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

public class Config {

    private static final Config instance = new Config();

    private List<String> extraModsDirs = null;
    private String mainModsDir = "mods";
    private String configDIr = "config";

    public static String getModListFile() {
        return null;
    }

    public static String getMainModsDir() {
        return instance.mainModsDir;
    }

    public static List<String> getExtraModsDirs() {
        return instance.extraModsDirs;
    }

    public static String getConfigDir() {
        return instance.configDIr;
    }

    public static void setExtraModsDirs(List<String> extraModsDirs) {
        if (extraModsDirs == null || extraModsDirs.isEmpty()) instance.extraModsDirs = null;
        else instance.extraModsDirs = extraModsDirs;
    }

    public static void setMainModsDir(String mainModsDir) {
        if (mainModsDir == null || mainModsDir.isEmpty()) instance.mainModsDir = "mods";
        else instance.mainModsDir = mainModsDir;
    }

    public static void setConfigDIr(String configDIr) {
        if (configDIr == null || configDIr.isEmpty()) instance.configDIr = "config";
        else instance.configDIr = configDIr;
    }

    public static void loadConfig() {
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
                        Agent.ConfigJson jsonObject = gsonParser.fromJson(json, Agent.ConfigJson.class);
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
}
