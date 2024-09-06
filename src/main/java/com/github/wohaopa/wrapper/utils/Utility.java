package com.github.wohaopa.wrapper.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.Nonnull;

import com.github.wohaopa.wrapper.Agent;
import com.github.wohaopa.wrapper.Tags;

public class Utility {

    public static byte[] getReplacementClassBytes(String className) {
        try (InputStream is = Utility.class.getResourceAsStream("/resources/" + className + ".class")) {
            if (is == null) {
                throw new IOException("Replacement class not found.");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            WrapperLog.log.warning("No replacement class found: " + className);
            WrapperLog.log.warning(e.getMessage());
        }
        return null;
    }

    public static void saveClass(@Nonnull String className, byte[] classfileBuffer) {
        String outputClassFile = className + ".class";
        File file = new File(Tags.saveDir, outputClassFile);
        File dir = file.getParentFile();
        if (!dir.exists()) dir.mkdirs();

        if (!file.exists() || file.delete()) try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(classfileBuffer);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean parseDir(String dir) {
        if (dir == null || dir.isEmpty()) return false;
        File file = new File(dir);
        if (!file.exists()) return file.mkdirs();
        else return file.isDirectory();
    }

    private static String jarFilePath;

    public static void exportFileFromJar(File destFile, String name) throws IOException {
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

}
