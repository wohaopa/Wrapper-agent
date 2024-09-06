package com.github.wohaopa.wrapper.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JSONUtility {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
        .create();

    public static void saveToFile(Object obj, File file, Type type) {
        try {
            CharSink a = Files.asCharSink(file, Charsets.UTF_8);
            if (type != null) a.write(gson.toJson(obj, type));
            else a.write(gson.toJson(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T loadFromFile(File file, Type typeOfT) {
        String context;
        try {
            context = Files.asCharSource(file, Charsets.UTF_8)
                .read()
                .replace("\r\n", "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return gson.fromJson(context, typeOfT);
    }

    public static <T> T loadFromFile(File file, Class<T> classOfT) {
        String context;
        try {
            context = Files.asCharSource(file, Charsets.UTF_8)
                .read()
                .replace("\r\n", "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return gson.fromJson(context, classOfT);
    }
}
