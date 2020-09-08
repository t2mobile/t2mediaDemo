package com.hf.t2mediademo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Util {
    //private static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private static SimpleDateFormat sDateFormat = new SimpleDateFormat("MMddHHmmssSSS");
    private static String EXT_VIDEO = "mp4";
    private static String EXT_SNAPSHOT = "jpg";
    private static SimpleDateFormat sTimeFormat = new SimpleDateFormat("HH:mm:ss");

    public static String filePath(String prefix, String ext) {
        return String.format(Locale.ENGLISH, "/sdcard/t2stream/%s_%s.%s", prefix, sDateFormat.format(new Date()), ext);
    }

    public static String videoFilePath(String streamId) {
        return filePath(String.format(Locale.ENGLISH, "R[%s]", streamId), EXT_VIDEO);
    }

    public static String snapshotFilePath() {
        return filePath("I", EXT_SNAPSHOT);
    }

    public static void prepareDir(String path) {
        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    public static String currentTimeStr() {
        return sTimeFormat.format(new Date());
    }
}
