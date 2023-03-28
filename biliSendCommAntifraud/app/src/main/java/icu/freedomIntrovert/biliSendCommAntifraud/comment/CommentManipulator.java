package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.net.Uri;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BandCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;

public class CommentManipulator {

    private OkHttpClient httpClient;
    private OkHttpClient httpClientNoRedirects;
    private String cookie;

    public CommentManipulator(OkHttpClient httpClient, String cookie) {
        this.httpClient = httpClient;
        httpClientNoRedirects = new OkHttpClient.Builder().followRedirects(false).build();
        this.cookie = cookie;

    }

    private String getCsrfFromCookie() {
        int csrfIndex = cookie.indexOf("bili_jct=");
        return cookie.substring(csrfIndex + 9, csrfIndex + 32 + 9);
    }

    public String bvidToOid(String bvid) throws IOException {
        Request request = new Request.Builder().url("http://api.bilibili.com/x/web-interface/view?bvid=" + bvid).build();
        Response response = httpClient.newCall(request).execute();
        String aid = null;
        if (response.code() == 200) {
            JSONObject respJson = JSON.parseObject(response.body().string());
            if (respJson.getInteger("code") == 0) {
                aid = respJson.getJSONObject("data").getString("aid");
            }
        }
        return aid;
    }

    public CommentArea dvidToCommentArea(String dvid) throws IOException {
        Request request = new Request.Builder()
                .url("http://api.bilibili.com/x/polymer/web-dynamic/v1/detail?id=" + dvid)
                //设置referer,不然会被拦截请求
                .addHeader("Referer", "https://t.bilibili.com/").build();
        Response response = httpClient.newCall(request).execute();
        String aid = null;
        int comment_type = CommentArea.AREA_TYPE_DYNAMIC11;
        if (response.code() == 200) {
            JSONObject respJson = JSON.parseObject(response.body().string());
            if (respJson.getInteger("code") == 0) {
                aid = respJson.getJSONObject("data").getJSONObject("item").getJSONObject("basic").getString("comment_id_str");
                comment_type = respJson.getJSONObject("data").getJSONObject("item").getJSONObject("basic").getInteger("comment_type");
            }
        }
        if (aid != null) {
            return new CommentArea(aid, dvid, comment_type);
        } else {
            return null;
        }

    }

    public JSONObject sendComment(String comment, CommentArea commentArea, String parent, String root) throws IOException {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        names.add("csrf");
        values.add(getCsrfFromCookie());
        names.add("message");
        values.add(Uri.encode(comment));
        names.add("oid");
        values.add(commentArea.oid);
        names.add("plat");
        values.add("1");
        names.add("type");
        values.add(String.valueOf(commentArea.areaType));
        if (parent != null) {
            names.add("parent");
            values.add(parent);
            names.add("root");
            values.add(root);
        }
        RequestBody requestBody = new FormBody(names, values);
        Request request = new Request.Builder()
                .url("https://api.bilibili.com/x/v2/reply/add")
                .post(requestBody)
                .header("cookie", cookie)
                .build();
        Response response = httpClient.newCall(request).execute();
        JSONObject callBack = JSON.parseObject(response.body().string());
        Log.i("sendCommentCallBack", callBack.toJSONString());
        return callBack;
    }

    public static long getRpidInSendRespJSON(JSONObject jsonObject) {
        return jsonObject.getJSONObject("data").getLong("rpid");
    }

    public JSONObject requestComment(CommentArea commentArea, int next, boolean hasCookie) throws IOException {
        Request.Builder builder = new Request.Builder().url("https://api.bilibili.com/x/v2/reply/main?mode=2&next=" + next + "&oid=" + commentArea.oid + "&plat=1&seek_rpid=&type=" + commentArea.areaType);
        if (hasCookie) {
            builder.addHeader("cookie", cookie);
        }
        Response response = httpClient.newCall(builder.build()).execute();
        JSONObject jsonObject = JSON.parseObject(response.body().string());
        Log.i("requestCommentCallBack", jsonObject.toJSONString());
        return jsonObject;
    }

    public JSONObject deleteComment(CommentArea commentArea, long rpid) throws IOException {
        RequestBody requestBody = new FormBody.Builder()
                .add("csrf", getCsrfFromCookie())
                .add("oid", commentArea.oid)
                .add("rpid", String.valueOf(rpid))
                .add("type", String.valueOf(commentArea.areaType))
                .build();
        Request request = new Request.Builder()
                .url("https://api.bilibili.com/x/v2/reply/del")
                .addHeader("cookie", cookie)
                .post(requestBody)
                .build();
        Log.i("deleteComment", "rpid:" + rpid);
        return JSON.parseObject(httpClient.newCall(request).execute().body().string());
    }

    public boolean checkComment(CommentArea commentArea, long rpid) throws IOException {
        JSONArray replies;
        replies = requestComment(commentArea, 0, false).getJSONObject("data").getJSONArray("replies");
        if (replies != null) {
            for (int i = 0; i < replies.size(); i++) {
                if (replies.getJSONObject(i).getLong("rpid") == rpid) {
                    return true;
                }
            }
        }
        return false;
    }

    public CommentArea matchCommentArea(String input) throws IOException {
        if (input.startsWith("BV")) {
            if (bvidToOid(input) != null) {
                return new CommentArea(bvidToOid(input), input, CommentArea.AREA_TYPE_VIDEO);
            } else {
                return null;
            }
        } else if (input.startsWith("cv")) {
            //cv号就相当于oid
            return new CommentArea(input.substring(2, 10), input, CommentArea.AREA_TYPE_ARTICLE);
        }

        if (input.startsWith("https://b23.tv/")) {
            Request request = new Request.Builder().url(input).build();
            Response response = httpClientNoRedirects.newCall(request).execute();
            input = response.header("Location");
            System.out.println(input);
        }

        if (input.startsWith("https://www.bilibili.com/video/") || input.startsWith("https://m.bilibili.com/video/") || input.startsWith("http://www.bilibili.com/video/") || input.startsWith("http://m.bilibili.com/video/")) {
            String sourceId = subUrl(input, "/video/", 12);
            if (bvidToOid(sourceId) != null) {
                return new CommentArea(bvidToOid(sourceId), sourceId, CommentArea.AREA_TYPE_VIDEO);
            } else {
                return null;
            }
        } else if (input.startsWith("https://www.bilibili.com/read/cv") || input.startsWith("http://www.bilibili.com/read/cv")) {
            String sourceId = subUrl(input, "/read/cv", 8);
            return new CommentArea(sourceId, "cv" + sourceId, CommentArea.AREA_TYPE_ARTICLE);
        } else if (input.startsWith("https://www.bilibili.com/read/mobile?id=")) {
            String sourceId = subUrl(input, "/read/mobile?id=", 8);
            return new CommentArea(sourceId, "cv" + sourceId, CommentArea.AREA_TYPE_ARTICLE);
        } else if (input.startsWith("https://www.bilibili.com/read/mobile/")) {
            String sourceId = subUrl(input, "/read/mobile/", 8);
            return new CommentArea(sourceId, "cv" + sourceId, CommentArea.AREA_TYPE_ARTICLE);
        } else if (input.startsWith("https://t.bilibili.com/")) {
            return dvidToCommentArea(subUrl(input, "t.bilibili.com/", 18));
        } else if (input.startsWith("https://m.bilibili.com/opus/")) {
            String sourceId = subUrl(input, "/opus/", 18);
            return dvidToCommentArea(sourceId);
        } else if (input.startsWith("https://m.bilibili.com/dynamic/")) {
            String sourceId = subUrl(input, "/dynamic/", 18);
            return dvidToCommentArea(sourceId);
        }
        return null;
    }

    private String subUrl(String url, String text, int length) {
        String subText = url.substring(url.indexOf(text) + text.length(), url.indexOf(text) + text.length() + length);
        System.out.println(subText);
        return subText;
    }

    public MartialLawCommentArea getMartialLawCommentArea(CommentArea commentArea, long testCommentRpid, String randomComment) throws IOException {
        byte[] coverImageData = null;
        String title = null, up = null;
        JSONObject callBack = sendComment(randomComment, commentArea, String.valueOf(testCommentRpid), String.valueOf(testCommentRpid));
        if (commentArea.areaType == CommentArea.AREA_TYPE_VIDEO) {
            Request request = new Request.Builder().url("http://api.bilibili.com/x/web-interface/view?bvid=" + commentArea.sourceId).build();
            Response response = httpClient.newCall(request).execute();
            if (response.code() == 200) {
                JSONObject respJson = JSON.parseObject(response.body().string());
                if (respJson.getInteger("code") == 0) {
                    //限制图片高为300px下载图片
                    String picUrl = respJson.getJSONObject("data").getString("pic");
                    Request request1 = new Request.Builder().url(picUrl + "@300h").build();
                    Response response1 = httpClient.newCall(request1).execute();
                    coverImageData = response1.body().bytes();
                    title = respJson.getJSONObject("data").getString("title");
                    up = respJson.getJSONObject("data").getJSONObject("owner").getString("name");
                }
            }
        } else if (commentArea.areaType == CommentArea.AREA_TYPE_ARTICLE) {
            Request request = new Request.Builder().url("https://api.bilibili.com/x/article/viewinfo?id=" + commentArea.oid).build();
            Response response = httpClient.newCall(request).execute();
            if (response.code() == 200) {
                JSONObject respJson = JSON.parseObject(response.body().string());
                if (respJson.getInteger("code") == 0) {
                    String picUrl = respJson.getJSONObject("data").getJSONArray("image_urls").getString(0);
                    Request request1 = new Request.Builder().url(picUrl + "@480w_300h_1c.jpg").build();
                    Response response1 = httpClient.newCall(request1).execute();
                    coverImageData = response1.body().bytes();
                    title = respJson.getJSONObject("data").getString("title");
                    up = respJson.getJSONObject("data").getString("author_name");
                }
            }
        } else if (commentArea.areaType == CommentArea.AREA_TYPE_DYNAMIC11 || commentArea.areaType == CommentArea.AREA_TYPE_DYNAMIC17) {
            Request request = new Request.Builder()
                    .url("http://api.bilibili.com/x/polymer/web-dynamic/v1/detail?id=" + commentArea.sourceId)
                    //设置referer,不然会被拦截请求
                    .addHeader("Referer", "https://t.bilibili.com/").build();
            Response response = httpClient.newCall(request).execute();
            if (response.code() == 200) {
                JSONObject respJson = JSON.parseObject(response.body().string());
                if (respJson.getInteger("code") == 0) {
                    up = respJson.getJSONObject("data").getJSONObject("item").getJSONObject("modules").getJSONObject("module_author").getString("name");
                    title = respJson.getJSONObject("data").getJSONObject("item").getJSONObject("modules").getJSONObject("module_dynamic").getJSONObject("desc").getString("text");
                    if (title.length() > 80) {
                        title = title.substring(0, 80);
                        title += "……";
                    }
                }
            }
        } else {
            return null;
        }
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String defaultDisposalMethod = null;
        if (callBack.getInteger("code") == 12022) {
            defaultDisposalMethod = BandCommentBean.BANNED_TYPE_QUICK_DELETE;
        } else {
            defaultDisposalMethod = BandCommentBean.BANNED_TYPE_SHADOW_BAN;
            deleteComment(commentArea, callBack.getJSONObject("data").getLong("rpid"));
        }
        return new MartialLawCommentArea(commentArea, defaultDisposalMethod, title, up, coverImageData);
    }

    public JSONObject appealComment(String id,String reason) throws IOException {
        String idType = "oid";
        if(id.startsWith("http")){
            idType = "url";
        }
        RequestBody requestBody = new FormBody.Builder()
                .add("csrf",getCsrfFromCookie())
                .add(idType,id)
                .add("type","1")
                .add("reason",reason)
                .build();
        Request request = new Request.Builder()
                .url("https://api.bilibili.com/x/v2/reply/appeal/submit")
                .header("cookie",cookie)
                .post(requestBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        JSONObject respJson = JSON.parseObject(response.body().string());
        Log.i("appealCommentResp",respJson.toJSONString());
        return respJson;
    }

}
