package icu.freedomIntrovert.biliSendCommAntifraud.picturestorage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PictureStorage {
    private static final Map<String,Bitmap> bitmapMap = new HashMap<>();

    private static final LinkedList<String> bitmapLink = new LinkedList<>();
    public synchronized static boolean save(Context context,String url) throws IOException {
        File pictureFile = new File(getPicturesDir(context),getPicFileNameFromUrl(url));
        if (!pictureFile.exists()) {
            OkHttpClient httpClient = OkHttpUtil.getHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()){
                return false;
            }
            ResponseBody body = response.body();
            OkHttpUtil.respNotNull(body);
            InputStream inputStream = body.byteStream();
            FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1){
                fileOutputStream.write(buffer,0,read);
            }
            fileOutputStream.close();
        }
        return true;
    }

    public synchronized static void save(Context context,InputStream inputStream,String name) throws IOException {
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream(new File(getPicturesDir(context),name));
        int read;
        while ((read = inputStream.read(buffer)) > -1){
            fos.write(buffer,0,read);
        }
        fos.close();
    }

    public synchronized static Bitmap getBitMap(String url){
        Bitmap bitmap = bitmapMap.get(url);
        if (bitmap != null) {
            bitmapLink.remove(url);
            bitmapLink.addFirst(url);
        }
        return bitmap;
    }

    public synchronized static boolean loadImage(Context context,String url) {
        File pictureFile = new File(getPicturesDir(context),getPicFileNameFromUrl(url));
        if (pictureFile.exists()){
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(pictureFile);
            } catch (FileNotFoundException e) {
                return false;
            }
            bitmapMap.put(url, BitmapFactory.decodeStream(fileInputStream));
            bitmapLink.addFirst(url);
            if (bitmapLink.size() > 25){
                bitmapMap.remove(bitmapLink.getLast());
                bitmapLink.removeLast();
            }
            return true;
        } else {
            return false;
        }
    }

    public synchronized static void delete(Context context,String url){
        File pictureFile = new File(getPicturesDir(context), getPicFileNameFromUrl(url));
        if (pictureFile.exists()){
            pictureFile.delete();
        }
    }

    public static File getPictureFile(Context context,String url){
        File pictureFile = new File(getPicturesDir(context), getPicFileNameFromUrl(url));
        //若图片存在才返回文件
        if (pictureFile.exists()){
            return pictureFile;
        }
        return null;
    }

    public static File getPicturesDir(Context context){
        return context.getExternalFilesDir("pictures");
    }


    private static String getPicFileNameFromUrl(String url){
        String[] split = url.split("/");
        return split[split.length-1];
    }
}
