package icu.freedomIntrovert.biliSendCommAntifraud.docmenthelper;

import android.content.Intent;

import java.io.File;

public class ActivityResultForFile extends ActivityResult{
    public ActivityResultForFile(Intent intent, File file) {
        super(intent);
        this.file = file;
    }
    public File file;
}
