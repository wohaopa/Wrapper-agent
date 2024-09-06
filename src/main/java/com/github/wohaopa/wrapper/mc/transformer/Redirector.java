package com.github.wohaopa.wrapper.mc.transformer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.launchwrapper.Launch;

import com.github.wohaopa.wrapper.Config;
import com.google.common.io.Files;

import cpw.mods.fml.relauncher.ModListHelper;

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
            if (jar == null) {
                jar = ModListHelper.additionalMods.values()
                    .stream()
                    .map(File::toPath)
                    .filter(filter)
                    .map(Path::toFile)
                    .findFirst()
                    .orElse(null);
            }

            if (jar == null) {
                try {
                    Field jarNamePrefixLowercaseField = mod.getClass()
                        .getField("jarNamePrefixLowercase");
                    jarNamePrefixLowercaseField.setAccessible(true);
                    String jarNamePrefixLowercase = (String) jarNamePrefixLowercaseField.get(mod);
                    String uid = Config.rename.get(jarNamePrefixLowercase);
                    if (uid != null) jar = ModListHelper.additionalMods.get(uid);

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            return jar;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<File> modFiles() {
        List<File> files = new LinkedList<>();

        try {
            files.addAll(
                java.nio.file.Files.walk(MOD_DIRECTORY_PATH)
                    .map(Path::toFile)
                    .collect(Collectors.toList()));
            for (Path path : EXTRA_MOD_DIRECTORY_PATH) {
                files.addAll(
                    java.nio.file.Files.walk(path)
                        .map(Path::toFile)
                        .collect(Collectors.toList()));
            }
            files.addAll(ModListHelper.additionalMods.values());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }
}
