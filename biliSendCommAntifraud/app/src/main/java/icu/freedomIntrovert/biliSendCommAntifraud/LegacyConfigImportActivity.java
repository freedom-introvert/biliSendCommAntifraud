package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Explicit recovery of an exported legacy config.xml; never scans protected framework paths. */
public final class LegacyConfigImportActivity extends Activity {
    private static final int PICK = 7;
    private static final Map<String, String> TYPES = new LinkedHashMap<>();
    static {
        TYPES.put("recordeHistory", "boolean");
        TYPES.put("cookie", "string");
        TYPES.put("deputy_cookie", "string");
        TYPES.put("wait_time", "long");
        TYPES.put("wait_time_by_danmaku_sent", "long");
        TYPES.put("wait_time_by_has_pictures", "long");
        TYPES.put("autoRecorde", "boolean");
        TYPES.put("sort_ruler", "int");
        TYPES.put("filter_ruler_enable_normal", "boolean");
        TYPES.put("filter_ruler_enable_shadow_ban", "boolean");
        TYPES.put("filter_ruler_enable_deleted", "boolean");
        TYPES.put("filter_ruler_enable_other", "boolean");
        TYPES.put("filter_ruler_enable_type1", "boolean");
        TYPES.put("filter_ruler_enable_type12", "boolean");
        TYPES.put("filter_ruler_enable_type11", "boolean");
        TYPES.put("filter_ruler_enable_type17", "boolean");
        TYPES.put("random_comments", "string");
        TYPES.put("forward_dynamic_url", "string");
        TYPES.put("forward_dynamic_id", "string");
        TYPES.put("花里胡哨", "boolean");
        TYPES.put("use_client_cookie", "boolean");
        TYPES.put("post_picture_hook", "boolean");
        TYPES.put("fuck_fold_pictures_hook", "boolean");
        TYPES.put("batch_check_interval", "long");
        TYPES.put("last_comment_locator_mode", "int");
    }
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) new AlertDialog.Builder(this)
                .setTitle("导入旧版设置")
                .setMessage("选择从旧版或文件管理器导出的 config.xml。仅恢复已知设置，不导入账号数据库或历史评论；同名设置会覆盖。旧框架私有目录中的设置不会自动迁出。")
                .setPositiveButton("选择文件", (d, w) -> startActivityForResult(
                        new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("*/*"), PICK))
                .setNegativeButton("取消", (d, w) -> finish())
                .setOnCancelListener(d -> finish()).show();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK) return;
        if (result != RESULT_OK || data == null || data.getData() == null) { finish(); return; }
        Uri uri = data.getData();
        worker.execute(() -> {
            try {
                Map<String, Object> values = parse(uri);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    new AlertDialog.Builder(this).setTitle("确认恢复设置")
                            .setMessage("已读取 " + values.size() + " 个已知设置。恢复前会在应用私有目录保留本次覆盖前的设置副本。")
                            .setPositiveButton("恢复", (d, w) -> worker.execute(() -> restore(values)))
                            .setNegativeButton("取消", (d, w) -> finish())
                            .setOnCancelListener(d -> finish()).show();
                });
            } catch (Exception e) { message("导入失败：文件不是有效的已知设置 XML，或没有读取权限。"); }
        });
    }

    private Map<String, Object> parse(Uri uri) throws Exception {
        byte[] bytes;
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalArgumentException("No input");
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > 1024 * 1024) throw new IllegalArgumentException("Too large");
                out.write(buffer, 0, count);
            }
            bytes = out.toByteArray();
        }
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(bytes), null);
        Map<String, Object> values = new LinkedHashMap<>();
        boolean root = false;
        int event;
        while ((event = parser.nextToken()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.DOCDECL || event == XmlPullParser.ENTITY_REF)
                throw new IllegalArgumentException("Entities not supported");
            if (event != XmlPullParser.START_TAG) continue;
            if (parser.getDepth() == 1) {
                if (!"map".equals(parser.getName())) throw new IllegalArgumentException("Wrong root");
                root = true;
                continue;
            }
            if (parser.getDepth() != 2) throw new IllegalArgumentException("Nested setting");
            String key = parser.getAttributeValue(null, "name");
            String type = TYPES.get(key);
            if (type == null) continue; // Migration markers and unrelated old keys are not imported.
            if (!type.equals(parser.getName())) throw new IllegalArgumentException("Wrong type");
            String raw = parser.getAttributeValue(null, "value");
            Object value;
            switch (type) {
                case "string": value = parser.nextText(); break;
                case "boolean":
                    if (!"true".equals(raw) && !"false".equals(raw)) throw new IllegalArgumentException("Boolean");
                    value = Boolean.valueOf(raw); break;
                case "int": value = Integer.valueOf(raw); break;
                case "long": value = Long.valueOf(raw); break;
                case "float": value = Float.valueOf(raw); break;
                default: throw new IllegalArgumentException("Unknown type");
            }
            values.put(key, value);
        }
        if (!root || values.isEmpty()) throw new IllegalArgumentException("No known settings");
        return values;
    }

    private void restore(Map<String, Object> values) {
        try {
            SharedPreferences local = Config.getInstance(this).sp;
            SharedPreferences backup = getSharedPreferences("config_before_api102_import_" + System.currentTimeMillis(), MODE_PRIVATE);
            SharedPreferences.Editor old = backup.edit();
            for (Map.Entry<String, ?> e : local.getAll().entrySet()) put(old, e.getKey(), e.getValue());
            if (!old.commit()) throw new IllegalStateException("Backup failed");
            SharedPreferences.Editor editor = local.edit();
            for (Map.Entry<String, Object> e : values.entrySet()) put(editor, e.getKey(), e.getValue());
            if (!editor.commit()) throw new IllegalStateException("Save failed");
            AntifraudApplication.get().publish();
            message("设置已恢复。重新打开首页可刷新开关；框架连接后同步 Hook 设置。");
        } catch (RuntimeException e) { message("保存失败；未确认恢复完成，请重试。"); }
    }

    private static void put(SharedPreferences.Editor e, String key, Object value) {
        if (value instanceof Boolean) e.putBoolean(key, (Boolean) value);
        else if (value instanceof String) e.putString(key, (String) value);
        else if (value instanceof Long) e.putLong(key, (Long) value);
        else if (value instanceof Integer) e.putInt(key, (Integer) value);
        else if (value instanceof Float) e.putFloat(key, (Float) value);
    }

    private void message(String text) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            new AlertDialog.Builder(this).setMessage(text).setPositiveButton("关闭", (d, w) -> finish())
                    .setOnCancelListener(d -> finish()).show();
        });
    }
    @Override protected void onDestroy() { super.onDestroy(); worker.shutdown(); }
}
