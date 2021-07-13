package com.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * init some configuration by classpath:cacheInit.json
 */
public class FileLoadUtils {

    public static String load(String fileName) {
        URL url = FileLoadUtils.class.getClassLoader().getResource(fileName);
        StringBuilder sb = new StringBuilder();
        if (url != null) {
            try {
                String builtInExpr = IOUtils.toString(url, StandardCharsets.UTF_8);
                sb.append(builtInExpr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
