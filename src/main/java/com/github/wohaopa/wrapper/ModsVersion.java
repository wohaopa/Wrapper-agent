package com.github.wohaopa.wrapper;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.reflect.TypeToken;

public class ModsVersion {

    static final Type type = new TypeToken<List<_ModsVersion>>() {}.getType();

    private List<_ModsVersion> versions;
    private Map<String, _ModsVersion> versionMap = new HashMap<>();
    private final File file;

    public ModsVersion(File file) {
        this.file = file;
    }

    public boolean load() {
        if (!file.isFile()) return false;
        versions = JSONUtility.loadFromFile(file, type);;
        if (versions == null) return false;
        for (_ModsVersion version : versions) {
            versionMap.put(version.name, version);
        }
        return true;
    }

    public Set<String> getMods() {
        return versionMap.keySet();
    }

    public List<String> getVersions(String mod) {
        return versionMap.get(mod).versions;
    }

    public String getPath(String mod) {
        return versionMap.get(mod).path;
    }

    private static class _ModsVersion {

        public String name;
        public String path;
        public List<String> versions;
    }
}
