package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.Reflect;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.HookConfig;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;

public final class PostPictureHook extends BaseHook {
    private final java.util.Set<Activity> picking =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    @Override public void startHook(int version, ClassLoader loader) throws Throwable {
        Class<?> chooser = loader.loadClass("com.bilibili.bplus.following.publish.view.MediaChooserActivity");
        hook(Reflect.declaredOrInherited(chooser, Activity.class, "startActivityForResult",
                Intent.class, int.class), chain -> {
            if (!chooser.isInstance(chain.getThisObject())) return chain.proceed();
            Object[] args = chain.getArgs().toArray();
            if (HookConfig.replacePicture() && Integer.valueOf(1000).equals(args[1])) {
                args[0] = new Intent(Intent.ACTION_PICK).setType("image/*");
                picking.add((Activity) chain.getThisObject());
            }
            return chain.proceed(args);
        });
        hook(Reflect.declaredOrInherited(chooser, Activity.class, "onActivityResult",
                int.class, int.class, Intent.class), chain -> {
            if (!chooser.isInstance(chain.getThisObject())) return chain.proceed();
            Object[] args = chain.getArgs().toArray();
            Activity activity = (Activity) chain.getThisObject();
            if (Integer.valueOf(1000).equals(args[0]) && picking.remove(activity)) {
                onPicked(activity, args);
            }
            return chain.proceed();
        });
    }

    private void onPicked(Object activityObject, Object[] args) throws Throwable {
                Activity activity = (Activity) activityObject;
                Intent intent = (Intent) args[2];
                if ((Integer) args[0] == 1000) {
                    if (intent != null && intent.getData() != null) {
                        //复制文件到缓存路径
                        File boxing = new File(activity.getExternalCacheDir(),"boxing");
                        if (!boxing.exists()){
                            if (!boxing.mkdirs()) {
                                XB.log("创建路径 "+boxing+" 失败");
                                return;
                            }
                        }
                        String fileName = new File(boxing,System.currentTimeMillis() + ".jpg").getAbsolutePath();
                        try (InputStream inputStream = activity.getContentResolver().openInputStream(intent.getData());
                             FileOutputStream fos = new FileOutputStream(fileName)) {
                            if (inputStream == null) {
                                XB.log("无法打开输入流，复制照片文件失败！");
                                return;
                            }
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = inputStream.read(buffer)) > -1) {
                                fos.write(buffer, 0, read);
                            }
                            XB.log("复制照片完毕！");
                            for (Method declaredMethod : activity.getClass().getDeclaredMethods()) {
                                Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                                /*
                                某方法作用为设置图片路径，复制好照片传入路径调用即可
                                但由于混淆方法名，每个版本方法名可能不一样，
                                不过MediaChooserActivity只有一个参数为(String)的方法，也就是目标方法
                                 */
                                if (Arrays.equals(new Class[]{String.class}, parameterTypes)) {
                                    Reflect.callMethod(activity, declaredMethod.getName(), fileName);
                                }
                            }
                        } catch (IOException e) {
                            XB.log("复制照片文件失败，异常信息：" + e);
                        }
                    } else {
                        //没选择照片就退出，不然会停留纯黑Activity
                        activity.finish();
                    }
                }
    }
}
