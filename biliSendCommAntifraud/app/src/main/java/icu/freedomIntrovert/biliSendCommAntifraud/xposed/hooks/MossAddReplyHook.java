package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.Reflect;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;

/** Captures gRPC/Moss addReply used by some 6.4.0 opus/comment3 paths. */
public final class MossAddReplyHook extends BaseHook {
    private volatile java.lang.ref.WeakReference<Activity> resumed = new java.lang.ref.WeakReference<>(null);
    private volatile Context hostContext;

    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws Throwable {
        hook(Activity.class, "onResume", chain -> {
            Object result = chain.proceed();
            Activity activity = (Activity) chain.getThisObject();
            resumed = new java.lang.ref.WeakReference<>(activity);
            hostContext = activity.getApplicationContext();
            return result;
        });
        Class<?> moss;
        try {
            moss = classLoader.loadClass("com.bapis.bilibili.main.community.reply.v1.ReplyMoss");
        } catch (ClassNotFoundException e) {
            XB.log("event=hook_skipped feature=MossAddReplyHook reason=no_ReplyMoss");
            moss = null;
        }
        if (moss != null) {
            Class<?> reqClz = classLoader.loadClass("com.bapis.bilibili.main.community.reply.v1.AddReplyReq");
            Class<?> handlerClz = classLoader.loadClass("com.bilibili.lib.moss.api.MossResponseHandler");
            hook(moss, "executeAddReply", chain -> {
                Object resp = chain.proceed();
                try {
                    dispatchMoss(chain.getArg(0), resp);
                } catch (Throwable failure) {
                    XB.error("event=comment_capture_failed kind=moss_execute", failure);
                }
                return resp;
            }, reqClz);
            hook(moss, "addReply", chain -> {
                Object req = chain.getArg(0);
                Object handler = chain.getArg(1);
                Object wrapped = wrapHandler(handlerClz, handler, req);
                Object[] args = chain.getArgs().toArray();
                args[1] = wrapped;
                return chain.proceed(args);
            }, reqClz, handlerClz);
        }
        hookKReplyMoss(classLoader);
    }

    private void hookKReplyMoss(ClassLoader classLoader) throws Throwable {
        Class<?> kmoss;
        try {
            kmoss = classLoader.loadClass("com.bapis.bilibili.main.community.reply.v1.KReplyMoss");
        } catch (ClassNotFoundException e) {
            XB.log("event=hook_skipped feature=KReplyMoss reason=no_KReplyMoss");
            return;
        }
        Class<?> handlerClz = null;
        for (Method method : kmoss.getDeclaredMethods()) {
            if (!"addReply".equals(method.getName()) || method.getParameterCount() != 2) continue;
            Class<?>[] types = method.getParameterTypes();
            if (types[0].getName().endsWith("KAddReplyReq")) {
                if (hasOnNext(types[1])) {
                    handlerClz = types[1];
                    break;
                }
            }
        }
        if (handlerClz == null) {
            XB.log("event=hook_skipped feature=KReplyMoss reason=no_handler_overload");
            return;
        }
        final Class<?> kHandlerClz = handlerClz;
        for (Method method : kmoss.getDeclaredMethods()) {
            if (!"addReply".equals(method.getName())) continue;
            if ((method.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) continue;
            Class<?>[] types = method.getParameterTypes();
            int handlerIndex = -1;
            for (int i = 0; i < types.length; i++) {
                if (types[i] == kHandlerClz) {
                    handlerIndex = i;
                    break;
                }
            }
            if (handlerIndex < 0) continue;
            final int index = handlerIndex;
            hook(method, chain -> {
                Object[] args = chain.getArgs().toArray();
                args[index] = wrapKHandler(kHandlerClz, args[index], args[0]);
                return chain.proceed(args);
            });
        }
    }

    private Object wrapHandler(Class<?> handlerClz, Object handler, Object req) {
        if (handler == null) return null;
        InvocationHandler proxy = (Object p, Method method, Object[] args) -> {
            if ("onNext".equals(method.getName()) && args != null && args.length >= 1) {
                try {
                    dispatchMoss(req, args[0]);
                } catch (Throwable failure) {
                    XB.error("event=comment_capture_failed kind=moss_onNext", failure);
                }
            }
            return method.invoke(handler, args);
        };
        return Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                new Class<?>[]{handlerClz}, proxy);
    }

    private Object wrapKHandler(Class<?> handlerClz, Object handler, Object req) {
        if (handler == null) return null;
        InvocationHandler proxy = (Object p, Method method, Object[] args) -> {
            if ("onNext".equals(method.getName()) && args != null && args.length >= 1) {
                try {
                    dispatchKMoss(req, args[0]);
                } catch (Throwable failure) {
                    XB.error("event=comment_capture_failed kind=kmoss_onNext", failure);
                }
            } else if ("onError".equals(method.getName()) && args != null && args.length >= 1
                    && args[0] instanceof Throwable) {
                try {
                    dispatchKMossError(req, (Throwable) args[0]);
                } catch (Throwable failure) {
                    XB.error("event=comment_capture_failed kind=kmoss_onError", failure);
                }
            }
            return method.invoke(handler, args);
        };
        ClassLoader loader = handler.getClass().getClassLoader();
        Class<?>[] interfaces = handlerClz.isInterface()
                ? new Class<?>[]{handlerClz}
                : handler.getClass().getInterfaces();
        if (interfaces.length == 0) interfaces = new Class<?>[]{handlerClz};
        return Proxy.newProxyInstance(loader, interfaces, proxy);
    }

    private void dispatchMoss(Object req, Object resp) {
        if (resp == null) return;
        Object reply;
        try {
            reply = Reflect.callMethod(resp, "getReply");
        } catch (RuntimeException e) {
            return;
        }
        if (reply == null) return;
        long rpid = Reflect.getLongField(reply, "id_");
        if (rpid == 0) rpid = asLong(Reflect.callMethod(reply, "getId"));
        if (rpid == 0 || !CaptureDeduper.SHARED.firstSeen(CaptureDeduper.checkKey(rpid))) {
            XB.log("event=comment_skipped kind=moss rpid=" + rpid);
            return;
        }
        long oid = asLong(Reflect.callMethod(reply, "getOid"));
        int type = (int) asLong(Reflect.callMethod(reply, "getType"));
        long root = asLong(Reflect.callMethod(reply, "getRoot"));
        long parent = asLong(Reflect.callMethod(reply, "getParent"));
        long uid = asLong(Reflect.callMethod(reply, "getMid"));
        long ctime = asLong(Reflect.callMethod(reply, "getCtime"));
        String text = "";
        try {
            Object content = Reflect.callMethod(reply, "getContent");
            if (content != null) text = String.valueOf(Reflect.callMethod(content, "getMessage"));
        } catch (RuntimeException ignored) {}
        if ((oid == 0 || type == 0) && req != null) {
            try {
                if (oid == 0) oid = asLong(Reflect.callMethod(req, "getOid"));
                if (type == 0) type = (int) asLong(Reflect.callMethod(req, "getType"));
                if (text.isEmpty()) text = String.valueOf(Reflect.callMethod(req, "getMessage"));
            } catch (RuntimeException ignored) {}
        }
        Activity activity = resumed.get();
        Bundle extras = new Bundle();
        extras.putInt("action", ByXposedLaunchedActivity.ACTION_CHECK_COMMENT);
        extras.putLong("oid", oid);
        extras.putInt("type", type);
        extras.putLong("rpid", rpid);
        extras.putLong("root", root);
        extras.putLong("parent", parent);
        extras.putString("comment_text", text);
        String sourceId = String.valueOf(oid);
        if (type == 11 && activity != null) {
            String dyn = PostCommentHook.getDynamic11ID(activity);
            if (dyn != null) sourceId = dyn;
        }
        extras.putString("source_id", sourceId);
        extras.putLong("uid", uid);
        extras.putLong("ctime", ctime);
        XB.log("event=comment_captured kind=moss rpid=" + rpid + " oid=" + oid + " type=" + type);
        Utils.startActivity(activity != null ? activity : hostContext, extras);
    }

    private void dispatchKMoss(Object req, Object resp) {
        if (resp == null) return;
        KReplyCapture.Snapshot snap = KReplyCapture.fromKAdd(req, resp);
        launchCheck(snap);
    }

    private void dispatchKMossError(Object req, Throwable error) {
        if (!KReplyCapture.looksLikeSensitive(error)) {
            XB.log("event=comment_skipped kind=kmoss_error");
            return;
        }
        KReplyCapture.Snapshot snap = KReplyCapture.fromKAdd(req, null);
        Activity activity = resumed.get();
        if (!CaptureDeduper.SHARED.firstSeen(CaptureDeduper.sensitiveKey(snap.oid, snap.type, snap.text))) {
            XB.log("event=comment_skipped kind=kmoss_sensitive oid=" + snap.oid + " type=" + snap.type);
            return;
        }
        Bundle extras = new Bundle();
        extras.putInt("action", ByXposedLaunchedActivity.ACTION_SAVE_CONTAIN_SENSITIVE_CONTENT);
        extras.putLong("oid", snap.oid);
        extras.putInt("type", snap.type);
        extras.putString("comment_text", snap.text);
        extras.putString("source_id", sourceId(activity, snap.type, snap.oid));
        extras.putString("toast_message", error.getMessage());
        XB.log("event=comment_captured kind=kmoss_sensitive oid=" + snap.oid + " type=" + snap.type);
        Utils.startActivity(activity != null ? activity : hostContext, extras);
    }

    private void launchCheck(KReplyCapture.Snapshot snap) {
        if (!snap.hasRpid() || !CaptureDeduper.SHARED.firstSeen(CaptureDeduper.checkKey(snap.rpid))) {
            XB.log("event=comment_skipped kind=kmoss rpid=" + snap.rpid);
            return;
        }
        Activity activity = resumed.get();
        Bundle extras = new Bundle();
        extras.putInt("action", ByXposedLaunchedActivity.ACTION_CHECK_COMMENT);
        extras.putLong("oid", snap.oid);
        extras.putInt("type", snap.type);
        extras.putLong("rpid", snap.rpid);
        extras.putLong("root", snap.root);
        extras.putLong("parent", snap.parent);
        extras.putString("comment_text", snap.text);
        extras.putString("source_id", sourceId(activity, snap.type, snap.oid));
        extras.putLong("uid", snap.uid);
        extras.putLong("ctime", snap.ctime);
        XB.log("event=comment_captured kind=kmoss rpid=" + snap.rpid + " oid=" + snap.oid + " type=" + snap.type);
        Utils.startActivity(activity != null ? activity : hostContext, extras);
    }

    private static String sourceId(Activity activity, int type, long oid) {
        String sourceId = String.valueOf(oid);
        if (type == 11 && activity != null) {
            String dyn = PostCommentHook.getDynamic11ID(activity);
            if (dyn != null) sourceId = dyn;
        }
        return sourceId;
    }

    private static boolean hasOnNext(Class<?> type) {
        for (Method method : type.getMethods()) {
            if ("onNext".equals(method.getName()) && method.getParameterCount() == 1) return true;
        }
        return false;
    }

    private static long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
