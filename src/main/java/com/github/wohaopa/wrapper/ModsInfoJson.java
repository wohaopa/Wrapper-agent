package com.github.wohaopa.wrapper;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import cpw.mods.fml.relauncher.ModListHelper;

public class ModsInfoJson {

    static final Type type = new TypeToken<List<_ModsInfo>>() {}.getType();

    private final File jsonFile;
    private List<_ModsInfo> modsInfoList;
    private List<_ModsInfo> misModsInfoList;

    public ModsInfoJson(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    public boolean load() {
        if (!jsonFile.isFile()) return false;
        modsInfoList = JSONUtility.loadFromFile(jsonFile, type);;
        return modsInfoList != null;
    }

    public void saveMisMod() {
        JSONUtility
            .saveToFile(misModsInfoList, new File(jsonFile.getParentFile(), "Mis_mods_" + jsonFile.getName()), type);;
    }

    public void save() {
        JSONUtility.saveToFile(modsInfoList, jsonFile, type);;
    }

    public _ModsInfo getModsInfo(String name, String version) {
        if (modsInfoList == null) return null;
        version = ":" + version;
        for (_ModsInfo modsInfo : modsInfoList) {
            if (modsInfo.uid.equals(name) && modsInfo.id.endsWith(version)) return modsInfo;
        }
        return null;
    }

    public boolean check(File repo) {
        misModsInfoList = new ArrayList<>();
        if (!repo.isDirectory() || modsInfoList == null) {
            misModsInfoList.addAll(modsInfoList);
            return false;
        }
        for (_ModsInfo modsInfo : modsInfoList) {
            File file = new File(repo, modsInfo.path);
            if (!file.exists()) misModsInfoList.add(modsInfo);
        }
        return misModsInfoList.isEmpty();
    }

    public void migrate(File dir, File repo) {
        WrapperLog.log.info("Migrating " + dir.getAbsolutePath() + " to " + repo.getAbsolutePath());
        for (_ModsInfo modsInfo : modsInfoList) {
            File file = new File(dir, modsInfo.filename);
            if (!file.exists()) {
                WrapperLog.log.info("Skipping " + modsInfo.filename);
                continue;
            }
            File file2 = new File(repo, modsInfo.path);
            if (!file2.getParentFile()
                .exists())
                if (!file2.getParentFile()
                    .mkdirs()) {
                        WrapperLog.log.info("Skipping " + modsInfo.filename);
                        continue;
                    }
            file.renameTo(file2);
            WrapperLog.log.info("Migrated " + modsInfo.filename + " to " + file2.getAbsolutePath());
        }
        WrapperLog.log.info("Migrating Completed");
    }

    public void saveForgeModsListFile(File repo, File modsListInfoFile) {
        List<String> modRef = new ArrayList<>();
        for (_ModsInfo modsInfo : modsInfoList) {
            modRef.add(modsInfo.id);
        }

        ModListHelper.JsonModList jsonModList = new ModListHelper.JsonModList();
        jsonModList.modRef = modRef;
        jsonModList.parentList = null;
        jsonModList.repositoryRoot = repo.getAbsolutePath();

        JSONUtility.saveToFile(jsonModList, modsListInfoFile, null);
    }

    public List<_ModsInfo> getMisModsList() {
        return misModsInfoList;
    }

    public void replace(_ModsInfo modsInfo) {
        for (int i = 0; i < modsInfoList.size(); i++) {
            if (modsInfoList.get(i).id.equals(modsInfo.id)) {
                modsInfoList.set(i, modsInfo);
                return;
            }
        }
        modsInfoList.add(modsInfo);
    }

    public static class _ModsInfo {

        public String id;
        public String uid;
        public String filename;
        public String url;
        public String path;
        @SerializedName("private")
        public boolean isPrivate;
    }
}
