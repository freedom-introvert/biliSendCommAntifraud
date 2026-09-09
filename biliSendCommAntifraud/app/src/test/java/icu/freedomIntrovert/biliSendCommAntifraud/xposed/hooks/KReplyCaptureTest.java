package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import org.junit.Test;

import static org.junit.Assert.*;

public class KReplyCaptureTest {
    public static final class FakeContent {
        public String getMessage() { return "hello"; }
    }

    public static final class FakeReply {
        public long getId() { return 99L; }
        public long getOid() { return 11L; }
        public long getType() { return 11L; }
        public long getRoot() { return 3L; }
        public long getParent() { return 4L; }
        public long getMid() { return 5L; }
        public long getCtime() { return 6L; }
        public FakeContent getContent() { return new FakeContent(); }
    }

    public static final class FakeResp {
        public FakeReply getReply() { return new FakeReply(); }
    }

    public static final class FakeReq {
        public long getOid() { return 22L; }
        public long getType() { return 17L; }
        public String getMessage() { return "from-req"; }
        public long getRoot() { return 8L; }
        public long getParent() { return 9L; }
    }

    public static final class EmptyResp {
        public Object getReply() { return null; }
    }

    @Test public void readsReplyFieldsFromKAddResponse() {
        KReplyCapture.Snapshot snap = KReplyCapture.fromKAdd(new FakeReq(), new FakeResp());
        assertEquals(11L, snap.oid);
        assertEquals(11, snap.type);
        assertEquals(99L, snap.rpid);
        assertEquals(3L, snap.root);
        assertEquals(4L, snap.parent);
        assertEquals(5L, snap.uid);
        assertEquals(6L, snap.ctime);
        assertEquals("hello", snap.text);
        assertTrue(snap.hasRpid());
    }

    @Test public void fallsBackToRequestWhenReplyMissing() {
        KReplyCapture.Snapshot snap = KReplyCapture.fromKAdd(new FakeReq(), new EmptyResp());
        assertEquals(22L, snap.oid);
        assertEquals(17, snap.type);
        assertEquals(0L, snap.rpid);
        assertEquals(8L, snap.root);
        assertEquals(9L, snap.parent);
        assertEquals("from-req", snap.text);
        assertFalse(snap.hasRpid());
    }

    @Test public void detectsSensitiveBizCode() {
        assertTrue(KReplyCapture.looksLikeSensitive(new RuntimeException("biz 12016 filtered")));
        assertFalse(KReplyCapture.looksLikeSensitive(new RuntimeException("timeout")));
        assertFalse(KReplyCapture.looksLikeSensitive(null));
    }
}
