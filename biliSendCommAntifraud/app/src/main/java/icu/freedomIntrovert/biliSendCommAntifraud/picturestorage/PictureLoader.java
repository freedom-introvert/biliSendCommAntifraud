package icu.freedomIntrovert.biliSendCommAntifraud.picturestorage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import java.io.IOException;

import icu.freedomIntrovert.async.TaskManger;
import icu.freedomIntrovert.biliSendCommAntifraud.R;

public class PictureLoader {
    private final Context context;
    private String url;

    private PictureLoader(Context context) {
        this.context = context;
    }

    public static PictureLoader with(Context context) {
        return new PictureLoader(context);
    }

    public PictureLoader load(String url) {
        this.url = url;
        return this;
    }

    public PictureLoader into(ImageView imageView) {
        Bitmap bitmap = PictureStorage.getBitMap(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return this;
        }
        TaskManger.start(() -> {
            boolean loaded = PictureStorage.loadImage(context, url);
            if (loaded) {
                setBitMap(imageView,PictureStorage.getBitMap(url));
                return;
            }
            try {
                if (PictureStorage.save(context, url)) {
                    PictureStorage.loadImage(context, url);
                    setBitMap(imageView,PictureStorage.getBitMap(url));
                } else {
                    setFailed(imageView);
                }
            } catch (IOException e) {
                setFailed(imageView);
            }

        });

        return this;
    }

    private static void setBitMap(ImageView imageView,Bitmap bitmap){
        TaskManger.postOnUiThread(() -> imageView.setImageBitmap(bitmap));
    }

    private void setFailed(ImageView imageView){
        TaskManger.postOnUiThread(() -> {
            Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.picture_failed);
            imageView.setImageDrawable(drawable);
        });

    }


}
