package com.github.wohaopa.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

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
            e.printStackTrace();
        }
        return null;
    }

    public static <T> Set<T> createImmutableSet(T... elements) {
        Set<T> set = new HashSet<>();
        Collections.addAll(set, elements);
        return Collections.unmodifiableSet(set);
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
}
