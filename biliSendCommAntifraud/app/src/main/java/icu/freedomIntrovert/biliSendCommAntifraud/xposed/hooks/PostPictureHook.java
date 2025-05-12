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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;

public class PostPictureHook extends BaseHook {



    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        XposedHelpers.findAndHookMethod("com.bilibili.bplus.following.publish.view.MediaChooserActivity", classLoader, "startActivityForResult", android.content.Intent.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Intent intent = (Intent) param.args[0];
                int requestCode = (Integer) param.args[1];
                //劫持打开相机，转向至打开相册
                if (requestCode == 1000) {
                    Intent newIntent = new Intent();
                    newIntent.setAction(Intent.ACTION_PICK);
                    newIntent.setType("image/*");
                    param.args[0] = newIntent;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });

        XposedHelpers.findAndHookMethod("com.bilibili.bplus.following.publish.view.MediaChooserActivity", classLoader, "onActivityResult", int.class, int.class, android.content.Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Activity activity = (Activity) param.thisObject;
                Intent intent = (Intent) param.args[2];
                if ((Integer) param.args[0] == 1000) {
                    if (intent != null && intent.getData() != null) {
                        //复制文件到缓存路径
                        File boxing = new File(activity.getExternalCacheDir(),"boxing");
                        if (!boxing.exists()){
                            if (boxing.mkdir()) {
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
                                    XposedHelpers.callMethod(activity, declaredMethod.getName(), fileName);
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
        });
    }

}
