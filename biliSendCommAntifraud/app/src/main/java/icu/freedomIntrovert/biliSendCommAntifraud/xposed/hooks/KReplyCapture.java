package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import icu.freedomIntrovert.biliSendCommAntifraud.xposed.Reflect;

/** Parses KMP/KReplyMoss addReply req/resp without depending on host class names at compile time. */
public final class KReplyCapture {
    private KReplyCapture() {}

    public static final class Snapshot {
        public final long oid;
        public final int type;
        public final long rpid;
        public final long root;
        public final long parent;
        public final long uid;
        public final long ctime;
        public final String text;

        public Snapshot(long oid, int type, long rpid, long root, long parent,
                        long uid, long ctime, String text) {
            this.oid = oid;
            this.type = type;
            this.rpid = rpid;
            this.root = root;
            this.parent = parent;
            this.uid = uid;
            this.ctime = ctime;
            this.text = text == null ? "" : text;
        }

        public boolean hasRpid() {
            return rpid != 0;
        }
    }

    public static Snapshot fromKAdd(Object req, Object resp) {
        Object reply = getter(resp, "getReply");
        long rpid = longGetter(reply, "getId");
        long oid = longGetter(reply, "getOid");
        int type = (int) longGetter(reply, "getType");
        long root = longGetter(reply, "getRoot");
        long parent = longGetter(reply, "getParent");
        long uid = longGetter(reply, "getMid");
        long ctime = longGetter(reply, "getCtime");
        String text = stringGetter(getter(reply, "getContent"), "getMessage");
        if ((oid == 0 || type == 0 || text.isEmpty()) && req != null) {
            if (oid == 0) oid = longGetter(req, "getOid");
            if (type == 0) type = (int) longGetter(req, "getType");
            if (text.isEmpty()) text = stringGetter(req, "getMessage");
            if (root == 0) root = longGetter(req, "getRoot");
            if (parent == 0) parent = longGetter(req, "getParent");
        }
        return new Snapshot(oid, type, rpid, root, parent, uid, ctime, text);
    }

    public static boolean looksLikeSensitive(Throwable error) {
        if (error == null) return false;
        String message = error.getMessage();
        if (message != null && message.contains("12016")) return true;
        return String.valueOf(error).contains("12016");
    }

    static Object getter(Object target, String name) {
        if (target == null) return null;
        try {
            return Reflect.callMethod(target, name);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static long longGetter(Object target, String name) {
        Object value = getter(target, name);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    static String stringGetter(Object target, String name) {
        Object value = getter(target, name);
        return value == null ? "" : String.valueOf(value);
    }
}
