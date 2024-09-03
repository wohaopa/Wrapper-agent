package com.github.wohaopa.wrapper;

import java.io.File;

public class Tags {

    private Tags() {}

    public static final String VERSION = "GRADLETOKEN_VERSION";
    public static final String Name = "ZeroPointWrapper";

    public static final String wrapperRepo = "https://raw.githubusercontent.com/wohaopa/Wrapper/update/";
    public static final String modsVersionsPath = "mods-versions-wrapper.json";

    public static final File saveDir = new File(Name + "/saveClass").getAbsoluteFile();
    public static final File downloadDir = new File(Name + "/downloads").getAbsoluteFile();
    public static final File modsRepository = new File("ModsRepository").getAbsoluteFile();
}
