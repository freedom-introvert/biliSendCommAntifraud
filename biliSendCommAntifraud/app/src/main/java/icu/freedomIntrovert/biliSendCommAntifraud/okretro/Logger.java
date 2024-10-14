package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import icu.freedomIntrovert.biliSendCommAntifraud.BuildConfig;

public class Logger {
    public static final String TAG = "HttpLogger";
    private static final int MAX_LOG_FILES = 100;
    private static final String LOG_FILE_EXTENSION = ".log";

    private final File logDirectory;
    private PrintWriter logWriter;

    public Logger(File logDirectory) {
        this.logDirectory = logDirectory;
        if (!logDirectory.exists()){
            logDirectory.mkdirs();
        }
        createNewLogFile();
    }

    private void createNewLogFile() {
        // Create a new log file with the current timestamp
        String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date()) + LOG_FILE_EXTENSION;
        File logFile = new File(logDirectory, fileName);
        try {
            logFile.createNewFile();
            logWriter = new PrintWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        deleteOldLogFiles();
    }

    private void deleteOldLogFiles() {
        File[] logFiles = logDirectory.listFiles((dir, name) -> name.endsWith(LOG_FILE_EXTENSION));
        if (logFiles == null) return;
        // Sort log files based on their last modified timestamp in ascending order
        // (oldest log files first)
        Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));
        int numFilesToDelete = logFiles.length - MAX_LOG_FILES;
        if (numFilesToDelete > 0) {
            for (int i = 0; i < numFilesToDelete; i++) {
                logFiles[i].delete();
            }
        }
    }

    public synchronized void log(String message) {
        if (logWriter == null) return;
        logWriter.println(message);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message);
        }
        logWriter.flush();
    }

}
