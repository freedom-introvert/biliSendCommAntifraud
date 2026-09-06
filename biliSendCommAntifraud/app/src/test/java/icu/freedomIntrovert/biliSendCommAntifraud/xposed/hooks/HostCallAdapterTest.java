package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class HostCallAdapterTest {

    public static final class RetrofitResponse {
        public final Object b;
        RetrofitResponse(Object body) { this.b = body; }
    }

    public static final class OldResponse {
        public final Object b = "field-body";
        public Object a() { return "method-body"; }
    }

    public static final class HttpUrl {
        public final String i;
        HttpUrl(String url) { this.i = url; }
        @Override public String toString() { return i; }
    }

    public static final class EncodedForm {
        public final List<String> a;
        public final List<String> b;
        EncodedForm(List<String> names, List<String> values) {
            this.a = names;
            this.b = values;
        }
    }

    public static final class NamedForm {
        private final List<String> names;
        private final List<String> values;
        NamedForm(List<String> names, List<String> values) {
            this.names = names;
            this.values = values;
        }
        public int size() { return names.size(); }
        public String name(int i) { return names.get(i); }
        public String value(int i) { return values.get(i); }
    }

    public static final class ObfuscatedRequest {
        public final HttpUrl a;
        public final Object d;
        ObfuscatedRequest(HttpUrl url, Object body) {
            this.a = url;
            this.d = body;
        }
    }

    public static final class LegacyRequest {
        private final NamedForm body;
        private final String url;
        LegacyRequest(String url, NamedForm body) {
            this.url = url;
            this.body = body;
        }
        public NamedForm body() { return body; }
        public String url() { return url; }
    }

    public static final class ObfuscatedCall {
        public final ObfuscatedRequest b;
        ObfuscatedCall(ObfuscatedRequest request) { this.b = request; }
        public ObfuscatedRequest request() { return b; }
    }

    @Test public void readsRetrofitFieldBodyWhenNoAccessorExists() {
        assertEquals("general", HostCallAdapter.responseBodyOf(new RetrofitResponse("general"), "a"));
    }

    @Test public void prefersOldBodyMethodOverField() {
        assertEquals("method-body", HostCallAdapter.responseBodyOf(new OldResponse(), "a"));
    }

    @Test public void readsRequestFromMethodThenField() {
        ObfuscatedRequest request = new ObfuscatedRequest(
                new HttpUrl("https://api.bilibili.com/x/v2/reply/add"), null);
        assertSame(request, HostCallAdapter.requestOf(new ObfuscatedCall(request), "h"));
    }

    @Test public void filtersReplyAddUrl() {
        assertTrue(HostCallAdapter.isReplyAddUrl("https://api.bilibili.com/x/v2/reply/add"));
        assertFalse(HostCallAdapter.isReplyAddUrl("https://api.bilibili.com/x/v2/reply/reply"));
        assertFalse(HostCallAdapter.isReplyAddUrl(null));
    }

    @Test public void decodesObfuscatedFormListsWithoutUsingMissingNameApi() {
        EncodedForm form = new EncodedForm(
                Arrays.asList("oid", "type", "message"),
                Arrays.asList("1145141919810", "1", "%E4%BD%A0%E5%A5%BD"));
        ObfuscatedRequest request = new ObfuscatedRequest(
                new HttpUrl("https://api.bilibili.com/x/v2/reply/add"), form);
        Map<String, String> fields = HostCallAdapter.formFieldsOf(request);
        assertEquals("1145141919810", fields.get("oid"));
        assertEquals("1", fields.get("type"));
        assertEquals("你好", fields.get("message"));
        assertEquals("https://api.bilibili.com/x/v2/reply/add", HostCallAdapter.urlOf(request));
    }

    @Test public void keepsLegacyNameValueFormApi() {
        NamedForm form = new NamedForm(Arrays.asList("oid", "message"), Arrays.asList("1", "C++"));
        Map<String, String> fields = HostCallAdapter.formFieldsOf(new LegacyRequest(
                "https://api.bilibili.com/x/v2/reply/add", form));
        assertEquals("1", fields.get("oid"));
        assertEquals("C++", fields.get("message"));
    }

    @Test public void buildsGlobalVideoExtrasWithAidAndRoot() {
        Map<String, String> extras = HostCallAdapter.globalVideoExtras(
                114514L, 2L, 1L, "BV1xx411c7mD");
        assertEquals("114514", extras.get("aid"));
        assertEquals("114514", extras.get("id"));
        assertEquals("BV1xx411c7mD", extras.get("bvid"));
        assertEquals("1", extras.get("comment_root_id"));
        assertEquals("2", extras.get("comment_secondary_id"));
        assertEquals("1", extras.get("tab_index"));
        assertEquals("bilibili://video/114514", HostCallAdapter.globalVideoUri(114514L));
    }

    @Test public void rootCommentDoesNotSetSecondaryId() {
        Map<String, String> extras = HostCallAdapter.globalVideoExtras(1L, 9L, 0L, null);
        assertEquals("9", extras.get("comment_root_id"));
        assertFalse(extras.containsKey("comment_secondary_id"));
        assertFalse(extras.containsKey("bvid"));
    }

    @Test public void fallsBackBetweenLegacyAndUnitedVideoActivity() {
        assertEquals(HostCallAdapter.GLOBAL_VIDEO_ACTIVITY,
                HostCallAdapter.fallbackActivity(HostCallAdapter.LEGACY_VIDEO_ACTIVITY));
        assertEquals(HostCallAdapter.LEGACY_VIDEO_ACTIVITY,
                HostCallAdapter.fallbackActivity(HostCallAdapter.GLOBAL_VIDEO_ACTIVITY));
        assertNull(HostCallAdapter.fallbackActivity(HostCallAdapter.COMMENT_DETAIL_ACTIVITY));
    }

    @Test public void emptyFormDoesNotThrow() {
        assertEquals(Collections.emptyMap(), HostCallAdapter.formFieldsOf(
                new ObfuscatedRequest(new HttpUrl("https://example"), null)));
    }

    @Test public void prefersBvidFromExtrasOverAvidFallback() {
        assertEquals("BV1xx411c7mD", HostCallAdapter.videoSourceId(
                "BV1xx411c7mD", "114514", "bilibili://video/114514", 114514L));
        assertEquals("AV114514", HostCallAdapter.videoSourceId(
                null, "114514", "bilibili://video/114514", 114514L));
    }

    @Test public void readsDynamicIdFromFragmentOrDeepLink() {
        assertEquals("123", HostCallAdapter.dynamicIdFrom(null, "123", null, null, null));
        assertEquals("456", HostCallAdapter.dynamicIdFrom(
                null, null, null, "bilibili://following/detail/456", null));
    }

    @Test public void parsesOpusAndFollowingUris() {
        assertEquals("iyIF8Ij", HostCallAdapter.opusOrDynamicId("https://www.bilibili.com/opus/iyIF8Ij"));
        assertEquals("123456", HostCallAdapter.opusOrDynamicId("bilibili://opus/123456"));
        assertEquals("789", HostCallAdapter.opusOrDynamicId("bilibili://following/detail/789?from=feed"));
    }

    @Test public void recognizesVertexAndComposeHosts() {
        assertTrue(HostCallAdapter.isComposeOrVertexActivity(
                "kntr.common.compose.launcher.VertexActivity"));
        assertTrue(HostCallAdapter.isComposeOrVertexActivity(
                "kntr.common.compose.launcher.TranslucentVertexActivity"));
        assertTrue(HostCallAdapter.isComposeOrVertexActivity(
                "com.bilibili.lib.ui.ComposeActivity"));
        assertFalse(HostCallAdapter.isComposeOrVertexActivity(
                "tv.danmaku.bili.MainActivityV2"));
        assertFalse(HostCallAdapter.isComposeOrVertexActivity(null));
    }
}
