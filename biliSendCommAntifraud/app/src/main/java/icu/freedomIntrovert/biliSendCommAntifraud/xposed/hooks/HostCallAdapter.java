package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import icu.freedomIntrovert.biliSendCommAntifraud.xposed.Reflect;

public final class HostCallAdapter {
    public static final String GLOBAL_VIDEO_ACTIVITY =
            "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity";
    public static final String LEGACY_VIDEO_ACTIVITY =
            "com.bilibili.video.videodetail.VideoDetailsActivity";
    public static final String COMMENT_DETAIL_ACTIVITY =
            "com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity";
    public static final String REPLY_ADD_PATH = "/x/v2/reply/add";

    private HostCallAdapter() {}

    public static Object responseBodyOf(Object response, String methodName) {
        if (response == null) return null;
        if (methodName != null && Reflect.hasMethod(response.getClass(), methodName)) {
            return Reflect.callMethod(response, methodName);
        }
        if (Reflect.hasField(response, "b")) {
            return Reflect.getObjectField(response, "b");
        }
        return null;
    }

    public static Object requestOf(Object call, String methodName) {
        if (call == null) return null;
        if (methodName != null && Reflect.hasMethod(call.getClass(), methodName)) {
            return Reflect.callMethod(call, methodName);
        }
        if (Reflect.hasMethod(call.getClass(), "request")) {
            return Reflect.callMethod(call, "request");
        }
        if (Reflect.hasField(call, "b")) {
            return Reflect.getObjectField(call, "b");
        }
        return null;
    }

    public static Object requestBodyOf(Object request) {
        if (request == null) return null;
        if (Reflect.hasMethod(request.getClass(), "body")) {
            return Reflect.callMethod(request, "body");
        }
        if (Reflect.hasField(request, "d")) {
            return Reflect.getObjectField(request, "d");
        }
        return null;
    }

    public static String urlOf(Object request) {
        if (request == null) return null;
        if (Reflect.hasMethod(request.getClass(), "url")) {
            Object url = Reflect.callMethod(request, "url");
            return url == null ? null : String.valueOf(url);
        }
        if (!Reflect.hasField(request, "a")) {
            return null;
        }
        Object httpUrl = Reflect.getObjectField(request, "a");
        if (httpUrl == null) return null;
        if (httpUrl instanceof String) return (String) httpUrl;
        if (Reflect.hasField(httpUrl, "i")) {
            Object encoded = Reflect.getObjectField(httpUrl, "i");
            if (encoded != null) return String.valueOf(encoded);
        }
        return String.valueOf(httpUrl);
    }

    public static boolean isReplyAddUrl(String url) {
        return url != null && url.contains(REPLY_ADD_PATH);
    }

    public static boolean isComposeOrVertexActivity(String name) {
        return name != null && (name.contains("ComposeActivity") || name.contains("VertexActivity"));
    }

    public static Map<String, String> formFieldsOf(Object request) {
        Object body = requestBodyOf(request);
        if (body == null) return Collections.emptyMap();
        Map<String, String> fromLists = encodedFormLists(body);
        if (fromLists != null) return fromLists;
        return namedFormApi(body);
    }

    static Map<String, String> encodedFormLists(Object body) {
        if (!Reflect.hasField(body, "a") || !Reflect.hasField(body, "b")) {
            return null;
        }
        Object names = Reflect.getObjectField(body, "a");
        Object values = Reflect.getObjectField(body, "b");
        if (!(names instanceof List) || !(values instanceof List)) {
            return null;
        }
        List<?> nameList = (List<?>) names;
        List<?> valueList = (List<?>) values;
        int size = Math.min(nameList.size(), valueList.size());
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            Object name = nameList.get(i);
            Object value = valueList.get(i);
            if (name == null) continue;
            map.put(decodeForm(String.valueOf(name)),
                    value == null ? null : decodeForm(String.valueOf(value)));
        }
        return map;
    }

    static Map<String, String> namedFormApi(Object body) {
        if (!Reflect.hasMethod(body.getClass(), "size")
                || !Reflect.hasMethod(body.getClass(), "name", int.class)
                || !Reflect.hasMethod(body.getClass(), "value", int.class)) {
            return Collections.emptyMap();
        }
        int size = (Integer) Reflect.callMethod(body, "size");
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String name = (String) Reflect.callMethod(body, "name", i);
            String value = (String) Reflect.callMethod(body, "value", i);
            if (name != null) map.put(name, value);
        }
        return map;
    }

    public static Map<String, String> globalVideoExtras(long oid, long rpid, long root, String sourceId) {
        Map<String, String> extras = new LinkedHashMap<>();
        extras.put("id", String.valueOf(oid));
        extras.put("aid", String.valueOf(oid));
        if (sourceId != null && sourceId.length() >= 2
                && "BV".equalsIgnoreCase(sourceId.substring(0, 2))) {
            extras.put("bvid", sourceId);
        }
        if (root != 0) {
            extras.put("comment_root_id", String.valueOf(root));
            extras.put("comment_secondary_id", String.valueOf(rpid));
        } else {
            extras.put("comment_root_id", String.valueOf(rpid));
        }
        extras.put("comment_from_spmid", "im.notify-reply.0.0");
        extras.put("tab_index", "1");
        return extras;
    }

    public static String globalVideoUri(long oid) {
        return "bilibili://video/" + oid;
    }

    public static String videoSourceId(String bvid, String id, String dataUri, long oid) {
        if (isBv(bvid)) return bvid;
        if (isBv(id)) return id;
        String fromUri = lastPath(dataUri);
        if (isBv(fromUri)) return fromUri;
        return "AV" + oid;
    }

    public static String dynamicIdFrom(String dynamicId, String fragmentDynamicId,
                                       String fragmentOid, String targetUrl, String enterUri) {
        if (notBlank(dynamicId)) return dynamicId;
        if (notBlank(fragmentDynamicId)) return fragmentDynamicId;
        if (notBlank(fragmentOid)) return fragmentOid;
        String fromTarget = opusOrDynamicId(targetUrl);
        if (notBlank(fromTarget)) return fromTarget;
        String fromEnter = opusOrDynamicId(enterUri);
        if (notBlank(fromEnter)) return fromEnter;
        return null;
    }

    static boolean isBv(String value) {
        return value != null && value.length() >= 2 && "BV".equalsIgnoreCase(value.substring(0, 2));
    }

    static boolean notBlank(String value) {
        return value != null && !value.isEmpty();
    }

    static String lastPath(String uri) {
        if (uri == null || uri.isEmpty()) return null;
        int slash = uri.lastIndexOf('/');
        if (slash < 0 || slash == uri.length() - 1) return uri;
        String last = uri.substring(slash + 1);
        int query = last.indexOf('?');
        return query >= 0 ? last.substring(0, query) : last;
    }

    public static String opusOrDynamicId(String uri) {
        if (uri == null || uri.isEmpty()) return null;
        String normalized = uri;
        int query = normalized.indexOf('?');
        if (query >= 0) normalized = normalized.substring(0, query);
        String[] markers = {"/opus/", "opus/", "/following/detail/", "following/detail/", "/dynamic/", "/h5/dynamic/detail/"};
        for (String marker : markers) {
            int at = indexOfIgnoreCase(normalized, marker);
            if (at >= 0) {
                String rest = normalized.substring(at + marker.length());
                int slash = rest.indexOf('/');
                String id = slash >= 0 ? rest.substring(0, slash) : rest;
                return id.isEmpty() ? null : id;
            }
        }
        return lastPath(normalized);
    }

    static int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(java.util.Locale.ROOT)
                .indexOf(needle.toLowerCase(java.util.Locale.ROOT));
    }

    public static String resolveTransferActivity(ClassLoader loader, String requested)
            throws ClassNotFoundException {
        try {
            loader.loadClass(requested);
            return requested;
        } catch (ClassNotFoundException original) {
            String fallback = fallbackActivity(requested);
            if (fallback == null) throw original;
            loader.loadClass(fallback);
            return fallback;
        }
    }

    static String fallbackActivity(String requested) {
        if (LEGACY_VIDEO_ACTIVITY.equals(requested)) return GLOBAL_VIDEO_ACTIVITY;
        if (GLOBAL_VIDEO_ACTIVITY.equals(requested)) return LEGACY_VIDEO_ACTIVITY;
        return null;
    }

    static String decodeForm(String raw) {
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return raw;
        }
    }
}
