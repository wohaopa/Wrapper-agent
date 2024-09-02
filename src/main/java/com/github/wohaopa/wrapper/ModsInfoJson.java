package com.github.wohaopa.wrapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import cpw.mods.fml.relauncher.ModListHelper;

public class ModsInfoJson {

    static Map<File, List<_ModsInfo>> cache = new HashMap<>();

    public static List<_ModsInfo> load(File file) {
        if (!file.exists()) return null;
        if (cache.containsKey(file)) return cache.get(file);
        String context;
        try {
            context = Files.asCharSource(file, Charsets.UTF_8)
                .read()
                .replace("\r\n", "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Gson gson = new Gson();
        Type userListType = new TypeToken<List<_ModsInfo>>() {}.getType();
        List<_ModsInfo> list = gson.fromJson(context, userListType);
        cache.put(file, list);
        return list;
    }

    public static List<_ModsInfo> verifyFiles(File repo, List<_ModsInfo> modsInfoList) {
        if (!repo.exists()) return modsInfoList;
        List<_ModsInfo> failedModsInfoList = new ArrayList<>();
        for (_ModsInfo modsInfo : modsInfoList) {
            File file = new File(repo, modsInfo.path);
            if (!file.exists()) failedModsInfoList.add(modsInfo);
        }
        return failedModsInfoList;
    }

    public static void migrate(File dir, File repo, List<_ModsInfo> modsInfoList, File modsListInfoFile) {
        List<String> modRef = new ArrayList<>();
        for (_ModsInfo modsInfo : modsInfoList) {
            File file = new File(dir, modsInfo.filename);
            if (!file.exists()) continue;
            File file2 = new File(repo, modsInfo.path);
            if (!file2.getParentFile()
                .exists())
                file2.getParentFile()
                    .mkdirs();
            file.renameTo(file2);
            modRef.add(modsInfo.id);
        }

        ModListHelper.JsonModList jsonModList = new ModListHelper.JsonModList();
        jsonModList.modRef = modRef;
        jsonModList.parentList = null;
        jsonModList.repositoryRoot = repo.getAbsolutePath();

        Gson gson = new Gson();
        try {
            Files.asCharSink(modsListInfoFile, Charsets.UTF_8)
                .write(gson.toJson(jsonModList));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
