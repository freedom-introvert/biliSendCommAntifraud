package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import icu.freedomIntrovert.async.TaskManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReplyPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.ForwardDynamicReqObject;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.ForwardDynamicResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.RemoveDynamicReqObject;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
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
        if (cookie == null){
            return false;
        }
        return cookie.contains("bili_jct=");
    }

    public boolean deputyCookieAreSet() {
        if (deputyCookie == null){
            return false;
        }
        return deputyCookie.contains("bili_jct=");
    }

    public String getCsrfFromCookie(boolean isDeputy) {
        String cookie = isDeputy ? this.deputyCookie : this.cookie;
        int csrfIndex = cookie.indexOf("bili_jct=");
        return cookie.substring(csrfIndex + 9, csrfIndex + 32 + 9);
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
        arrayMap.put("csrf", getCsrfFromCookie(isDeputyAccount));
        arrayMap.put("message", comment);
        arrayMap.put("oid", String.valueOf(commentArea.oid));
        arrayMap.put("plat", "1");
        arrayMap.put("parent", String.valueOf(parent));
        arrayMap.put("root", String.valueOf(root));
        arrayMap.put("type", String.valueOf(commentArea.type));
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

    /*public boolean checkComment(CommentArea commentArea, long rpid) throws IOException {
        GeneralResponse<CommentPage> body = biliApiService.getCommentPageNoAccount(commentArea.oid, commentArea.type, 1, BiliApiService.COMMENT_SORT_BY_TIME).execute().body();
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
    }*/

    public BiliComment findComment(CommentArea commentArea, long rpid, long root) throws IOException, BiliBiliApiException {
        List<BiliComment> replies;
        if (root == 0) {
            //获取第一页评论查找就行了
            GeneralResponse<CommentPage> body = biliApiService
                    .getCommentPageNoAccount(commentArea.oid,
                            commentArea.type,
                            1,
                            BiliApiService.COMMENT_SORT_BY_TIME)
                    .execute()
                    .body();
            OkHttpUtil.respNotNull(body);
            replies = body.data.replies;
            if (replies != null && replies.size() > 0) {
                for (BiliComment reply : replies) {
                    if (reply.rpid == rpid){
                        return reply;
                    }
                }
            }
            return null;
        } else {
            return findCommentFromCommentReplyArea(commentArea, rpid, root, false);
        }
    }
    public BiliComment findCommentFromCommentReplyArea(CommentArea commentArea, long rpid, long root, boolean hasAccount) throws IOException, BiliBiliApiException {
        GeneralResponse<CommentPage> body;
        if (hasAccount){
            body = biliApiService.getCommentMainPageHasAccount(cookie,commentArea.oid,commentArea.type,BiliApiService.COMMENT_SORT_MODE_TIME,0,rpid).execute().body();
        } else {
            body = biliApiService.getCommentMainPageNoAccount(getBuvid3Cookie(),commentArea.oid,commentArea.type,BiliApiService.COMMENT_SORT_MODE_TIME,0,rpid).execute().body();
        }
        OkHttpUtil.respNotNull(body);
        if (!body.isSuccess()){
            throw new BiliBiliApiException(body,"根评论被删除或ShadowBan，无法获取回复列表");
        }
        List<BiliComment> comments = new ArrayList<>(body.data.replies);
        //有可能被顶置，所以把这个弄一起
        if (body.data.top_replies != null) {
            comments.addAll(body.data.top_replies);
        }
        //遍历根评论列表
        for (BiliComment comment : comments) {
            if (comment.rpid == root){
                //遍历评论回复预览列表
                List<BiliComment> replies = comment.replies;
                if (replies != null){
                    for (BiliComment reply : replies) {
                        if (reply.rpid == rpid){
                            return reply;
                        }
                    }
                }
            }
        }
        return null;
    }
    public BiliComment findCommentUsingSeekRpid(Comment comment,boolean hasAccount) throws IOException, BiliBiliApiException {
        CommentArea commentArea = comment.commentArea;
        GeneralResponse<CommentPage> body;
        if (hasAccount) {
            body = biliApiService.getCommentMainPageHasAccount(cookie,commentArea.oid,
                            commentArea.type,BiliApiService.COMMENT_SORT_MODE_TIME,
                            0,comment.rpid)
                    .execute().body();
        } else {
            body = biliApiService.getCommentMainPageNoAccount(getBuvid3Cookie(),commentArea.oid,
                            commentArea.type,BiliApiService.COMMENT_SORT_MODE_TIME,
                            0,comment.rpid)
                    .execute().body();
        }
        OkHttpUtil.respNotNull(body);
        if (body.isSuccess()) {
            List<BiliComment> comments = body.data.replies;
            if (comments == null || comments.size() == 0){
                return null;
            }

            for (BiliComment gotAComment : comments) {
                if (gotAComment.rpid == comment.rpid){
                    return gotAComment;
                }
            }
            //评论被置顶的情况
            List<BiliComment> topReplies = body.data.top_replies;
            if (topReplies == null || topReplies.size() == 0){
                return null;
            }
            for (BiliComment aTopComment : topReplies) {
                if (aTopComment.rpid == comment.rpid){
                    return aTopComment;
                }
            }
            return null;
        } else {
            throw new BiliBiliApiException(body,"获取评论列表(/reply/main)时发生错误");
        }
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

    public void matchCommentAreaInUi(String input,MatchCommentAreaCallBack callBack){
        TaskManger.start(() -> {
            try {
                CommentArea commentArea = matchCommentArea(input);
                TaskManger.postOnUiThread(() -> callBack.onMatchedArea(commentArea));
            } catch (IOException e) {
                TaskManger.postOnUiThread(() -> callBack.onNetworkError(e));
            }
        });
    }

    public interface MatchCommentAreaCallBack {
        void onNetworkError(IOException e);
        void onMatchedArea(CommentArea commentArea);
    }

    private String subUrl(String url, String text, int length) {
        String subText = url.substring(url.indexOf(text) + text.length(), url.indexOf(text) + text.length() + length);
        System.out.println(subText);
        return subText;
    }

    /**
     * 获取戒严评论区信息。调用方法前请勿删除测试评论
     * @param commentArea
     * @param testCommentRpid
     * @return
     * @throws IOException
     */
    public MartialLawCommentArea getMartialLawCommentArea(CommentArea commentArea, long testCommentRpid,boolean isDeputyAccount) throws IOException {
        byte[] coverImageData = null;
        String title = null, up = null;
        GeneralResponse<CommentReplyPage> resp = getCommentReplyHasAccount(commentArea, testCommentRpid, 1,isDeputyAccount);
        if (commentArea.type == CommentArea.AREA_TYPE_VIDEO) {
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
        } else if (commentArea.type == CommentArea.AREA_TYPE_ARTICLE) {
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
        } else if (commentArea.type == CommentArea.AREA_TYPE_DYNAMIC11 || commentArea.type == CommentArea.AREA_TYPE_DYNAMIC17) {
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
            defaultDisposalMethod = MartialLawCommentArea.DISPOSAL_METHOD_QUICK_DELETE;
        } else {
            defaultDisposalMethod = MartialLawCommentArea.DISPOSAL_METHOD_SHADOW_BAN;
        }
        return new MartialLawCommentArea(commentArea, defaultDisposalMethod, title, up, coverImageData);
    }

    public JSONObject appealComment(String id, String reason) throws IOException {

        String idType = "oid";
        if (id.startsWith("http")) {
            idType = "url";
        }
        RequestBody requestBody = new FormBody.Builder()
                .add("csrf", getCsrfFromCookie(false))
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

    public Call<GeneralResponse<Object>> createDeleteCommentCall(CommentArea commentArea, long rpid) {
        return biliApiService.deleteComment(getCookie(), getCsrfFromCookie(false), commentArea.oid, commentArea.type, rpid);
    }

    public GeneralResponse<Object> deleteComment(CommentArea commentArea, long rpid, boolean isDeputyAccount) throws IOException, BiliBiliApiException {
        GeneralResponse<Object> body = biliApiService.deleteComment(isDeputyAccount ? deputyCookie : getCookie(), getCsrfFromCookie(isDeputyAccount), commentArea.oid, commentArea.type, rpid).execute().body();
        OkHttpUtil.respNotNull(body);
        if (!body.isSuccess()){
            throw new BiliBiliApiException(body,String.format("[rpid=%s][cookie:uid=%s]评论删除失败！",rpid,getDedeUserID(isDeputyAccount)));
        }
        return body;
    }


    public GeneralResponse<CommentReplyPage> getCommentReplyNoAccount(CommentArea commentArea, long rootRpid, int pn) throws IOException {
        return biliApiService.getCommentReply(commentArea.oid, pn, 20, rootRpid, commentArea.type, 0).execute().body();
    }

    /*public GeneralResponse<CommentReplyPage> getCommentReplyHasAccount(CommentArea commentArea, long rootRpid, int pn) throws IOException {
        return biliApiService.getCommentReply(getCookie(), getCsrfFromCookie(), commentArea.oid, pn, 20, rootRpid, commentArea.type, 0).execute().body();
    }*/
    public GeneralResponse<CommentReplyPage> getCommentReplyHasAccount(CommentArea commentArea, long rootRpid, int pn, boolean isDeputyAccount) throws IOException {
        return biliApiService.getCommentReply(isDeputyAccount ? deputyCookie : cookie, getCsrfFromCookie(isDeputyAccount), commentArea.oid, pn, 20, rootRpid, commentArea.type, 0).execute().body();
    }

/*    public GeneralResponse<CommentReplyPage> getCommentReplyMainPageHasAccountUseSeekRpid(CommentArea commentArea, long rootRpid,long seekRpid,int pn, boolean isDeputyAccount) throws IOException {
        return biliApiService.getCommentReplyMainPageHasHasAccount(isDeputyAccount ? deputyCookie : cookie,commentArea.oid,commentArea.type,BiliApiService.COMMENT_SORT_MODE_TIME,pn,rootRpid,seekRpid).execute().body();
    }*/

    public ForwardDynamicResult forwardDynamicUsingSubAccount(@NonNull String dynamicId) throws IOException, BiliBiliApiException {
        GeneralResponse<ForwardDynamicResult> body = biliApiService.forwardDynamic(deputyCookie, "web",getCsrfFromCookie(true),ForwardDynamicReqObject.getInstance(getDedeUserID(true),dynamicId)).execute().body();
        OkHttpUtil.respNotNull(body);
        if (!body.isSuccess()){
            throw new BiliBiliApiException(body,"转发动态失败");
        }
        return body.data;
    }

    public void deleteDynamicUsingSubAccount(@NonNull String dynamicId) throws IOException, BiliBiliApiException {
        GeneralResponse<Object> response = biliApiService.removeDynamic(deputyCookie, "web", getCsrfFromCookie(true), new RemoveDynamicReqObject(dynamicId)).execute().body();
        OkHttpUtil.respNotNull(response);
        if (!response.isSuccess()){
            throw new BiliBiliApiException(response,"删除动态 "+dynamicId+" 失败");
        }
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

    public String getBuvid3Cookie() {
        String patternString = "buvid3=[^;]+";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(cookie);
        matcher.find();
        return matcher.group();
    }

    public String getDedeUserID(boolean isDeputy){
        String patternString = "DedeUserID=([^;]+)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(isDeputy ? deputyCookie : cookie);
        matcher.find();
        return matcher.group(1);

    }
}
