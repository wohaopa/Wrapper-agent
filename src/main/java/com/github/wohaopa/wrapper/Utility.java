package com.github.wohaopa.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
}
