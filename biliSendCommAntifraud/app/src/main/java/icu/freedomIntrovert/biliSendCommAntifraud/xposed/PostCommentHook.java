package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PostCommentHook implements IXposedHookLoadPackage {
    //public static Context CurrentContext;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("tv.danmaku.bili")) {
            int appVersionCode = systemContext().getPackageManager().getPackageInfo(loadPackageParam.packageName, 0).versionCode;
            XposedBridge.log("bilibili version code:" + appVersionCode);
            AtomicReference<Context> currentContext = new AtomicReference<>();
            AtomicReference<String> currentOid = new AtomicReference<>();
            AtomicReference<String> currentId = new AtomicReference<>();
            AtomicReference<String> currentAreaType = new AtomicReference<>();
            AtomicReference<String> currentComment = new AtomicReference<>();

            //获取哔哩动态的ID。很多版本都是这样，不做特定版本适配
            XposedHelpers.findAndHookMethod("com.bilibili.lib.ui.ComposeActivity", loadPackageParam.classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Method getIntentMethod = param.thisObject.getClass().getMethod("getIntent");
                    Intent intent = (Intent) getIntentMethod.invoke(param.thisObject);
                    Bundle fragment_args = intent.getExtras().getBundle("fragment_args");
                    XposedBridge.log(fragment_args.getString("oid"));
                    currentId.set(fragment_args.getString("oid"));
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

            if (appVersionCode <= 7270300){//适配版本: ? - 7.25.0 - 7.27.0

                XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.model.b", loadPackageParam.classLoader, "A", loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.comment2.CommentContext"), java.lang.String.class, long.class, long.class, long.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        Object commentContext = param.args[0];
                        Method getOid = commentContext.getClass().getMethod("getOid");
                        Method getType = commentContext.getClass().getMethod("getType");
                        currentComment.set((String) param.args[5]);
                        currentOid.set(String.valueOf(getOid.invoke(commentContext)));
                        currentAreaType.set(String.valueOf(getType.invoke(commentContext)));
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                    }
                });

                XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.inputv2.CommentPublisher", loadPackageParam.classLoader, "t", loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.opus.lightpublish.page.comment.c"), loadPackageParam.classLoader.loadClass("com.bilibili.okretro.GeneralResponse"), new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        Object generalResponse = param.args[1];
                        Object biliCommentAddResult = generalResponse.getClass().getField("data").get(generalResponse);
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                        Field contextField = param.thisObject.getClass().getDeclaredField("a");
                        contextField.setAccessible(true);
                        Context context = (Context) contextField.get(param.thisObject);
                        Class<?> biliCommentAddResultClass = biliCommentAddResult.getClass();
                        if ((Integer) biliCommentAddResultClass.getField("action").get(biliCommentAddResult) == 0) {
                            intent.putExtra("message", (String) biliCommentAddResultClass.getField("message").get(biliCommentAddResult));
                            intent.putExtra("oid", currentOid.get());
                            intent.putExtra("type", currentAreaType.get());
                            intent.putExtra("rpid", String.valueOf((Long) biliCommentAddResultClass.getField("rpid").get(biliCommentAddResult)));
                            intent.putExtra("root", String.valueOf((Long) biliCommentAddResultClass.getField("root").get(biliCommentAddResult)));
                            intent.putExtra("parent", String.valueOf((Long) biliCommentAddResultClass.getField("parent").get(biliCommentAddResult)));
                            intent.putExtra("comment", currentComment.get());
                            intent.putExtra("id", currentId.get());
                            XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                            context.startActivity(intent);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                    }
                });
            } else { //适配版本 7.28.0 - 7.29.0 - ?
                XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.model.b", loadPackageParam.classLoader, "z", loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.comment2.CommentContext"), java.lang.String.class, long.class, long.class, long.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        Object commentContext = param.args[0];
                        Method getOid = commentContext.getClass().getMethod("getOid");
                        Method getType = commentContext.getClass().getMethod("getType");
                        currentComment.set((String) param.args[5]);
                        currentOid.set(String.valueOf(getOid.invoke(commentContext)));
                        currentAreaType.set(String.valueOf(getType.invoke(commentContext)));
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                    }
                });

                XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.inputv2.CommentPublisher", loadPackageParam.classLoader, "t", loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.opus.lightpublish.page.comment.c"), loadPackageParam.classLoader.loadClass("com.bilibili.okretro.GeneralResponse"), new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        Object generalResponse = param.args[1];
                        Object biliCommentAddResult = generalResponse.getClass().getField("data").get(generalResponse);
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                        Field contextField = param.thisObject.getClass().getDeclaredField("a");
                        contextField.setAccessible(true);
                        Context context = (Context) contextField.get(param.thisObject);
                        Class<?> biliCommentAddResultClass = biliCommentAddResult.getClass();
                        if ((Integer) biliCommentAddResultClass.getField("action").get(biliCommentAddResult) == 0) {//不等于0很可能是up开启了评论精选之类的，toast提示你了，检查无意义也误判
                            intent.putExtra("message", (String) biliCommentAddResultClass.getField("message").get(biliCommentAddResult));
                            intent.putExtra("oid", currentOid.get());
                            intent.putExtra("type", currentAreaType.get());
                            intent.putExtra("rpid", String.valueOf((Long) biliCommentAddResultClass.getField("rpid").get(biliCommentAddResult)));
                            intent.putExtra("root", String.valueOf((Long) biliCommentAddResultClass.getField("root").get(biliCommentAddResult)));
                            intent.putExtra("parent", String.valueOf((Long) biliCommentAddResultClass.getField("parent").get(biliCommentAddResult)));
                            intent.putExtra("comment", currentComment.get());
                            intent.putExtra("id", currentId.get());
                            XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                            context.startActivity(intent);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                    }
                });
            }



        } else if (loadPackageParam.packageName.equals("com.bilibili.app.in")) {//适配版本：? - 3.16.0 - ?
            AtomicReference<Context> currentContext = new AtomicReference<>();
            AtomicReference<String> currentOid = new AtomicReference<>();
            AtomicReference<String> currentId = new AtomicReference<>();
            AtomicReference<String> currentAreaType = new AtomicReference<>();
            AtomicReference<String> currentComment = new AtomicReference<>();

            XposedHelpers.findAndHookMethod("com.bilibili.lib.ui.ComposeActivity", loadPackageParam.classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Method getIntentMethod = param.thisObject.getClass().getMethod("getIntent");
                    Intent intent = (Intent) getIntentMethod.invoke(param.thisObject);
                    Bundle fragment_args = intent.getExtras().getBundle("fragment_args");
                    currentId.set(fragment_args.getString("oid"));
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
/*
            XposedHelpers.findAndHookConstructor("com.bilibili.app.comm.comment2.input.a", loadPackageParam.classLoader, androidx.fragment.app.FragmentActivity.class, loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.comment2.CommentContext"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    currentContext.set((Context) param.args[0]);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

 */

            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.model.b", loadPackageParam.classLoader, "z", java.lang.String.class, long.class, int.class, long.class, long.class, int.class, int.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, long.class, int.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, long.class, java.lang.String.class, java.lang.String.class, loadPackageParam.classLoader.loadClass("java.util.Map"), loadPackageParam.classLoader.loadClass("no1.a"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    currentOid.set(String.valueOf(param.args[1]));
                    currentAreaType.set(String.valueOf(param.args[2]));
                    currentComment.set((String) param.args[8]);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

            XposedHelpers.findAndHookMethod("com.bilibili.app.comm.comment2.input.a", loadPackageParam.classLoader, "L", loadPackageParam.classLoader.loadClass("com.bilibili.okretro.GeneralResponse"), loadPackageParam.classLoader.loadClass("com.bilibili.app.comm.comment2.input.a$e"), loadPackageParam.classLoader.loadClass("dc.r"), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Object generalResponse = param.args[0];
                    Object biliCommentAddResult = generalResponse.getClass().getField("data").get(generalResponse);
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud", "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
                    Field contextField = param.thisObject.getClass().getDeclaredField("a");
                    contextField.setAccessible(true);
                    Context context = (Context) contextField.get(param.thisObject);
                    Class<?> biliCommentAddResultClass = biliCommentAddResult.getClass();
                    if ((Integer) biliCommentAddResultClass.getField("action").get(biliCommentAddResult) == 0) {//不等于0很可能是up开启了评论精选之类的，toast提示你了，检查无意义也误判
                        intent.putExtra("message", (String) biliCommentAddResultClass.getField("message").get(biliCommentAddResult));
                        intent.putExtra("oid", currentOid.get());
                        intent.putExtra("type", currentAreaType.get());
                        intent.putExtra("rpid", String.valueOf((Long) biliCommentAddResultClass.getField("rpid").get(biliCommentAddResult)));
                        intent.putExtra("root", String.valueOf((Long) biliCommentAddResultClass.getField("root").get(biliCommentAddResult)));
                        intent.putExtra("parent", String.valueOf((Long) biliCommentAddResultClass.getField("parent").get(biliCommentAddResult)));
                        intent.putExtra("comment", currentComment.get());
                        intent.putExtra("id", currentId.get());
                        XposedBridge.log("bilibili comment add result:" + intent.getExtras().toString());
                        context.startActivity(intent);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });
        }
    }

    public static Context systemContext() {
        Object obj = null;
        Class<?> findClassIfExists = XposedHelpers.findClass("android.app.ActivityThread", (ClassLoader) null);
        if (findClassIfExists != null) {
            obj = XposedHelpers.callStaticMethod(findClassIfExists, "currentActivityThread", Arrays.copyOf(new Object[0], 0));
        }
        return (Context) XposedHelpers.callMethod(obj, "getSystemContext", Arrays.copyOf(new Object[0], 0));
    }

}
