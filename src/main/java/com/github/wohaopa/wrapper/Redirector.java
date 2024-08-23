package com.github.wohaopa.wrapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.launchwrapper.Launch;

import com.google.common.io.Files;

public class Redirector {

    public static final Path MOD_DIRECTORY_PATH;
    public static final List<Path> EXTRA_MOD_DIRECTORY_PATH;

    static {
        MOD_DIRECTORY_PATH = new File(Launch.minecraftHome, Config.getMainModsDir()).toPath();
        EXTRA_MOD_DIRECTORY_PATH = new LinkedList<>();
        List<String> extraMods = Config.getExtraModsDirs();
        if (extraMods != null) for (String s : Config.getExtraModsDirs()) {
            EXTRA_MOD_DIRECTORY_PATH.add(new File(Launch.minecraftHome, s).toPath());
        }
    }

    public static File findJarOf(Object mod) {
        try {
            Predicate<Path> filter;
            if (mod instanceof String jarname) {
                filter = p -> {
                    final String filename = p.toString();
                    final String extension = Files.getFileExtension(filename);
                    return Files.getNameWithoutExtension(filename)
                        .contains(jarname) && ("jar".equals(extension) || "litemod".equals(extension));
                };
            } else {
                try {
                    Method isMatchingJarMethod = mod.getClass()
                        .getDeclaredMethod("isMatchingJar", Path.class);
                    filter = p -> {
                        try {
                            return (boolean) isMatchingJarMethod.invoke(mod, p);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            File jar = java.nio.file.Files.walk(MOD_DIRECTORY_PATH)
                .filter(filter)
                .map(Path::toFile)
                .findFirst()
                .orElse(null);

            if (jar == null) {
                for (Path path : Redirector.EXTRA_MOD_DIRECTORY_PATH) {
                    jar = java.nio.file.Files.walk(path)
                        .filter(filter)
                        .map(Path::toFile)
                        .findFirst()
                        .orElse(null);
                    if (jar != null) break;
                }
            }

            return jar;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
