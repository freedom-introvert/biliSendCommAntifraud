package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReply;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentScanResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class CommentManipulator {

    public OkHttpClient httpClient;
    private OkHttpClient httpClientNoRedirects;
    private BiliApiService biliApiService;
    private String cookie;
    private String deputyCookie;

    public CommentManipulator(String cookie,String deputyCookie) {
        this.httpClient = OkHttpUtil.getHttpClient();
        httpClientNoRedirects = new OkHttpClient.Builder().followRedirects(false).build();
        this.biliApiService = ServiceGenerator.createService(BiliApiService.class);
        this.cookie = cookie;
        this.deputyCookie = deputyCookie;
    }



    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getCookie() {
        return cookie;
    }

    public boolean cookieAreSet() {
        return cookie.contains("bili_jct=");
    }

    public boolean deputyCookieAreSet() {
        return deputyCookie.contains("bili_jct=");
    }

    public String getCsrfFromCookie() {
        int csrfIndex = cookie.indexOf("bili_jct=");
        return cookie.substring(csrfIndex + 9, csrfIndex + 32 + 9);
    }

    public String getCsrfFromDeputyCookie() {
        int csrfIndex = deputyCookie.indexOf("bili_jct=");
        return deputyCookie.substring(csrfIndex + 9, csrfIndex + 32 + 9);
    }

    public Call<GeneralResponse<VideoInfo>> getVideoInfoByAid(long aid) {
        return biliApiService.getVideoInfoByAid(aid);
    }

    public String bvidToOid(String bvid) throws IOException {
        Request request = new Request.Builder().url("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid).build();
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
                .url("https://api.bilibili.com/x/polymer/web-dynamic/v1/detail?id=" + dvid)
                //设置各种请求头,尤其是Referer和user-agent不然会被拦截请求:(
                .addHeader("accept","application/json, text/plain, */*")
                .addHeader("user-agent","Mozilla/5.0 (Linux; Android "+ Build.VERSION.RELEASE+"; "+Build.MODEL+" Build/"+Build.ID+") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.163 Mobile Safari/537.36")
                .addHeader("sec-ch-ua-platform","\"Android\"")
                .addHeader("sec-ch-ua","\"Android WebView\";v=\"119\", \"Chromium\";v=\"119\", \"Not?A_Brand\";v=\"24\"")
                .addHeader("sec-fetch-site","same-site")
                .addHeader("sec-fetch-mode","cors")
                .addHeader("sec-fetch-dest","empty")
                .addHeader("Cookie",cookie)
                .addHeader("Referer", "https://m.bilibili.com").build();
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
            return new CommentArea(Long.parseLong(aid), dvid, comment_type);
        } else {
            return null;
        }
    }
/*
    public CommentSendResult sendComment(String comment, CommentArea commentArea, String parent, String root) throws IOException {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        names.add("csrf");
        values.add(getCsrfFromCookie());
        names.add("message");
        values.add(Uri.encode(comment));
        names.add("oid");
        values.add(String.valueOf(commentArea.oid));
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
        String resp = response.body().string();
        Log.i("sendCommentResp", resp);
        return JSON.parseObject(resp, CommentSendResult.class);
    }
 */

    public Call<GeneralResponse<CommentAddResult>> sendComment(String comment, long parent, long root, CommentArea commentArea,boolean isDeputyAccount) {
        ArrayMap<String, String> arrayMap = new ArrayMap<>();
        arrayMap.put("csrf", isDeputyAccount ? getCsrfFromDeputyCookie() : getCsrfFromCookie());
        arrayMap.put("message", comment);
        arrayMap.put("oid", String.valueOf(commentArea.oid));
        arrayMap.put("plat", "1");
        arrayMap.put("parent", String.valueOf(parent));
        arrayMap.put("root", String.valueOf(root));
        arrayMap.put("type", String.valueOf(commentArea.areaType));
        return biliApiService.postComment(isDeputyAccount ? deputyCookie : getCookie(), arrayMap);
    }


    /*public JSONObject requestComments(CommentArea commentArea, int next, long root, boolean hasCookie) throws IOException {
        //String url = "https://api.bilibili.com/x/v2/reply/main?mode=2&next=" + next + "&oid=" + commentArea.oid + "&plat=1&seek_rpid=&type=" + commentArea.areaType;
        String url = "https://api.bilibili.com/x/v2/reply?sort=0&pn=" + (next + 1) + "&oid=" + commentArea.oid + "&plat=1&type=" + commentArea.areaType;
        if (root != 0) {
            url = "https://api.bilibili.com/x/v2/reply/reply?sort=0&oid=" + commentArea.oid + "&pn=" + (next + 1) + "&ps=20&root=" + root + "&type=" + commentArea.areaType;
        }
        Request.Builder builder = new Request.Builder().url(url);
        if (hasCookie) {
            builder.addHeader("cookie", cookie);
        }
        Response response = httpClient.newCall(builder.build()).execute();
        JSONObject jsonObject = JSON.parseObject(response.body().string());
        Log.i("requestCommentResp", jsonObject.toJSONString());
        return jsonObject;
    }*/

    public boolean checkComment(CommentArea commentArea, long rpid) throws IOException {
        GeneralResponse<CommentPage> body = biliApiService.getCommentPageNoAccount(commentArea.oid, commentArea.areaType, 1, BiliApiService.COMMENT_SORT_BY_TIME).execute().body();
        OkHttpUtil.respNotNull(body);
        List<BiliComment> comments = body.data.replies;
        if (comments != null && comments.size() > 0){
            for (int i = 0; i < comments.size(); i++) {
                if (comments.get(i).rpid == rpid) {
                    return true;
                }
            }
        }
        return false;
    }

    public CommentScanResult scanComment(CommentArea commentArea, long rpid, long root) throws IOException {
        List<BiliComment> replies;
        if (root == 0) {
            //获取第一页评论查找就行了
            GeneralResponse<CommentPage> body = biliApiService.getCommentPageNoAccount(commentArea.oid, commentArea.areaType, 1, BiliApiService.COMMENT_SORT_BY_TIME).execute().body();
            OkHttpUtil.respNotNull(body);
            replies = body.data.replies;
            if (replies != null && replies.size() > 0) {
                for (BiliComment reply : replies) {
                    if (reply.rpid == rpid){
                        return new CommentScanResult(true, reply.invisible);
                    }
                }
            }
            return new CommentScanResult(false, false);
        } else {
            BiliComment reply = findCommentFromCommentReplyArea(commentArea, rpid, root, false, null);
            if (reply != null){
                return new CommentScanResult(true,reply.invisible);
            }
            return new CommentScanResult(false, false);
        }
    }


    public BiliComment findCommentFromCommentReplyArea(CommentArea commentArea, long rpid, long root, boolean hasAccount, @Nullable PageTurnListener pageTurnListener) throws IOException {
        int pn = 1;
        GeneralResponse<CommentReply> body;
        if (hasAccount) {
            body = getCommentReplyHasAccount(commentArea, root, pn).execute().body();
        } else {
            body = getCommentReplyNoAccount(commentArea, root, pn).execute().body();
        }
        if (pageTurnListener != null) {
            pageTurnListener.onPageTurn(pn);
        }
        OkHttpUtil.respNotNull(body);
        if (body.isSuccess()) {
            List<BiliComment> replyComments = body.data.replies;
            while (replyComments != null && replyComments.size() > 0) {
                for (BiliComment replyComment : replyComments) {
                    if (replyComment.rpid == rpid) {
                        return replyComment;
                    }
                }
                pn++;
                if (hasAccount) {
                    body = getCommentReplyHasAccount(commentArea, root, pn).execute().body();
                } else {
                    body = getCommentReplyNoAccount(commentArea, root, pn).execute().body();
                }
                if (pageTurnListener != null) {
                    pageTurnListener.onPageTurn(pn);
                }
                OkHttpUtil.respNotNull(body);
                replyComments = body.data.replies;
            }
        } else {
            throw new RuntimeException("在获取回复评论前未检查根评论状态，发生错误：code=" + body.code + " message=" + body.message);
        }
        return null;
    }


    public BiliComment findThisCommentFromEntireCommentArea(CommentArea commentArea, long rpid, boolean hasAccount, @Nullable PageTurnListener pageTurnListener) throws IOException {
        int pn = 1;
        GeneralResponse<CommentPage> body;
        if (hasAccount) {
            body = biliApiService.getCommentPageHasAccount(cookie,getCsrfFromCookie(),0, commentArea.oid, pn,commentArea.areaType).execute().body();
        } else {
            body = biliApiService.getCommentPageNoAccount(commentArea.oid, commentArea.areaType, pn, 0).execute().body();
        }
        if (pageTurnListener != null) {
            pageTurnListener.onPageTurn(pn);
        }
        OkHttpUtil.respNotNull(body);
        if (body.isSuccess()) {
            List<BiliComment> replyComments = body.data.replies;
            if (body.data.top_replies != null) {
                replyComments.addAll(body.data.top_replies);
            }
            while (!(replyComments == null || replyComments.size() == 0)) {
                for (BiliComment replyComment : replyComments) {
                    if (replyComment.rpid == rpid) {
                        return replyComment;
                    }
                }
                pn++;
                if (hasAccount) {
                    body = biliApiService.getCommentPageHasAccount(cookie,getCsrfFromCookie(),0, commentArea.oid, pn,commentArea.areaType).execute().body();
                } else {
                    body = biliApiService.getCommentPageNoAccount(commentArea.oid, commentArea.areaType, pn,0).execute().body();
                }
                if (pageTurnListener != null) {
                    pageTurnListener.onPageTurn(pn);
                }
                OkHttpUtil.respNotNull(body);
                replyComments = body.data.replies;
            }
        } else {
            throw new RuntimeException("在获取回复评论前未检查跟评论状态，发生错误：code=" + body.code + " message=" + body.message);
        }
        return null;
    }

    public interface PageTurnListener {
        void onPageTurn(int page);
    }


    public CommentArea matchCommentArea(String input) throws IOException {
        if (input.startsWith("BV")) {
            if (bvidToOid(input) != null) {
                return new CommentArea(Long.parseLong(bvidToOid(input)), input, CommentArea.AREA_TYPE_VIDEO);
            } else {
                return null;
            }
        } else if (input.startsWith("cv")) {
            //cv号就相当于oid
            return new CommentArea((Long.parseLong(input.substring(2, 10))), input, CommentArea.AREA_TYPE_ARTICLE);
        }

        if (input.startsWith("https://b23.tv/")) {
            Request request = new Request.Builder().url(input).build();
            Response response = httpClientNoRedirects.newCall(request).execute();
            input = response.header("Location");
            System.out.println(input);
        }

        if (input.startsWith("https://www.bilibili.com/video/BV") || input.startsWith("https://m.bilibili.com/video/BV") || input.startsWith("http://www.bilibili.com/video/BV") || input.startsWith("http://m.bilibili.com/video/BV")) {
            String sourceId = subUrl(input, "/video/", 12);
            if (bvidToOid(sourceId) != null) {
                return new CommentArea(Long.parseLong(bvidToOid(sourceId)), sourceId, CommentArea.AREA_TYPE_VIDEO);
            } else {
                return null;
            }
        } else if (input.startsWith("https://www.bilibili.com/video/av") || input.startsWith("https://m.bilibili.com/video/av") || input.startsWith("http://www.bilibili.com/video/BV") || input.startsWith("http://m.bilibili.com/video/BV")) {
            String text = "/video/";
            String aid = input.substring(input.indexOf(text) + text.length());
            return new CommentArea(Long.parseLong(aid.substring(2)), aid, CommentArea.AREA_TYPE_VIDEO);
        } else if (input.startsWith("https://www.bilibili.com/read/cv") || input.startsWith("http://www.bilibili.com/read/cv")) {
            String sourceId = subUrl(input, "/read/cv", 8);
            return new CommentArea(Long.parseLong(sourceId), "cv" + sourceId, CommentArea.AREA_TYPE_ARTICLE);
        } else if (input.startsWith("https://www.bilibili.com/read/mobile?id=")) {
            String sourceId = subUrl(input, "/read/mobile?id=", 8);
            return new CommentArea(Long.parseLong(sourceId), "cv" + sourceId, CommentArea.AREA_TYPE_ARTICLE);
        } else if (input.startsWith("https://www.bilibili.com/read/mobile/")) {
            String sourceId = subUrl(input, "/read/mobile/", 8);
            return new CommentArea(Long.parseLong(sourceId), "cv" + sourceId, CommentArea.AREA_TYPE_ARTICLE);
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
        GeneralResponse<CommentReply> resp = getCommentReplyHasAccount(commentArea, testCommentRpid, 1).execute().body();
        if (commentArea.areaType == CommentArea.AREA_TYPE_VIDEO) {
            Request request = new Request.Builder().url("https://api.bilibili.com/x/web-interface/view?bvid=" + commentArea.sourceId).build();
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
                    .url("https://api.bilibili.com/x/polymer/web-dynamic/v1/detail?id=" + commentArea.sourceId)
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
        String defaultDisposalMethod = null;
        if (resp.code == CommentAddResult.CODE_DELETED) {
            defaultDisposalMethod = BannedCommentBean.BANNED_TYPE_QUICK_DELETE;
        } else {
            defaultDisposalMethod = BannedCommentBean.BANNED_TYPE_SHADOW_BAN;
        }
        return new MartialLawCommentArea(commentArea, defaultDisposalMethod, title, up, coverImageData);
    }

    public JSONObject appealComment(String id, String reason) throws IOException {

        String idType = "oid";
        if (id.startsWith("http")) {
            idType = "url";
        }
        RequestBody requestBody = new FormBody.Builder()
                .add("csrf", getCsrfFromCookie())
                .add(idType, id)
                .add("type", "1")
                .add("reason", reason)
                .build();
        Request request = new Request.Builder()
                .url("https://api.bilibili.com/x/v2/reply/appeal/submit")
                .header("cookie", cookie)
                .post(requestBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        JSONObject respJson = JSON.parseObject(response.body().string());
        Log.i("appealCommentResp", respJson.toJSONString());
        return respJson;
    }

    public Call<Void> deleteComment(CommentArea commentArea, long rpid) {
        return biliApiService.deleteComment(getCookie(), getCsrfFromCookie(), commentArea.oid, commentArea.areaType, rpid);
    }

    public Call<GeneralResponse<CommentReply>> getCommentReplyNoAccount(CommentArea commentArea, long rootRpid, int pn) {
        return biliApiService.getCommentReply(commentArea.oid, pn, 20, rootRpid, commentArea.areaType, 0);
    }

    public Call<GeneralResponse<CommentReply>> getCommentReplyHasAccount(CommentArea commentArea, long rootRpid, int pn) {
        return biliApiService.getCommentReply(getCookie(), getCsrfFromCookie(), commentArea.oid, pn, 20, rootRpid, commentArea.areaType, 0);
    }

    public boolean checkCookieNotFailed() throws IOException {
        Request request = new Request.Builder()
                .url("https://member.bilibili.com/x2/creative/h5/calendar/event?ts=0")
                .addHeader("Cookie", cookie)
                .build();
        ResponseBody body = httpClient.newCall(request).execute().body();
        OkHttpUtil.respNotNull(body);
        JSONObject userProfileJSON = JSON.parseObject(body.string());
        JSONObject userProfile = userProfileJSON.getJSONObject("data").getJSONObject("pfs");
        return userProfile != null;
    }

}
