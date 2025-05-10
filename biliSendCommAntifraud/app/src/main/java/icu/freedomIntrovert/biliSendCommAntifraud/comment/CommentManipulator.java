package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.os.Build;
import android.util.ArrayMap;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import icu.freedomIntrovert.async.TaskManger;
import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAppealResp;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReplyPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.ForwardDynamicReqObject;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.ForwardDynamicResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.MainApiCommentPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.PaginationStr;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.RemoveDynamicReqObject;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CommentManipulator {
    private static CommentManipulator instance;
    public final OkHttpClient httpClient;
    private final OkHttpClient httpClientNoRedirects;
    public final BiliApiService biliApiService;
    //private String cookie;
    //private String deputyCookie;

    private CommentManipulator() {
        this.httpClient = OkHttpUtil.getHttpClient();
        httpClientNoRedirects = new OkHttpClient.Builder().followRedirects(false).build();
        this.biliApiService = ServiceGenerator.createService(BiliApiService.class);
    }


    public static synchronized CommentManipulator getInstance() {
        if (instance == null) {
            instance = new CommentManipulator();
        }
        return instance;
    }

    public String getCsrfFromCookie(String cookie) {
        int csrfIndex = cookie.indexOf("bili_jct=");
        return cookie.substring(csrfIndex + 9, csrfIndex + 32 + 9);
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

    public CommentArea dvidToCommentArea(String dvid, Account account) throws IOException, BiliBiliApiException {
        Request request = new Request.Builder()
                .url("https://api.bilibili.com/x/polymer/web-dynamic/v1/detail?id=" + dvid)
                //设置各种请求头,尤其是Referer和user-agent不然会被拦截请求:(
//                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("user-agent", "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + " Build/" + Build.ID + ") " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.163 Mobile Safari/537.36")
                .addHeader("Cookie", account != null ? account.cookie : "")
                .addHeader("Referer", "https://t.bilibili.com/").build();
        Response response = httpClient.newCall(request).execute();

        String aid = null;
        int comment_type = CommentArea.AREA_TYPE_DYNAMIC11;
        if (response.code() == 200) {
            JSONObject respJson = JSON.parseObject(response.body().string());
            if (respJson.getInteger("code") == 0) {
                aid = respJson.getJSONObject("data").getJSONObject("item").getJSONObject("basic").getString("comment_id_str");
                comment_type = respJson.getJSONObject("data").getJSONObject("item").getJSONObject("basic").getInteger("comment_type");
            } else if (respJson.getInteger("code") == -352) {
                throw new BiliBiliApiException(respJson.getInteger("code"),
                        respJson.getString("message"),
                        account == null ? "-352错误，你没有设置账号，应该是没有cookie被拦截了……" : "-352错误，未知原因被拦截，随机选的账号：" + account);
            }
        }
        if (aid != null) {
            return new CommentArea(Long.parseLong(aid), dvid, comment_type);
        } else {
            return null;
        }
    }

    public CommentAddResult sendComment(String commentText, long parent, long root, CommentArea commentArea, Account account) throws IOException, BiliBiliApiException {
        String cookie = account.cookie;
        ArrayMap<String, String> map = new ArrayMap<>();
        map.put("csrf", getCsrfFromCookie(cookie));
        map.put("message", commentText);
        map.put("oid", String.valueOf(commentArea.oid));
        map.put("plat", "1");
        map.put("parent", String.valueOf(parent));
        map.put("root", String.valueOf(root));
        map.put("type", String.valueOf(commentArea.type));
        return biliApiService.postComment(cookie, map).success("发送评论失败，评论：" + commentText);
    }

    public BiliComment findComment(Comment comment, Account account) throws BiliBiliApiException, IOException {
        return findComment(comment.commentArea.oid, comment.commentArea.type, comment.rpid, comment.root, comment.date, account);
    }

    public BiliComment findComment(BiliComment comment, Account account) throws BiliBiliApiException, IOException {
        return findComment(comment.oid, comment.type, comment.rpid, comment.root, new Date(comment.ctime * 1000), account);
    }

    /**
     * 查找评论
     *
     * @param account（放心，是残废cookie）
     * @return 找到的评论，找不到为null
     * @throws IOException
     * @throws BiliBiliApiException
     */
    public BiliComment findComment(long oid, int type, long rpid, long root, Date sentTime, Account account) throws IOException, BiliBiliApiException {
        List<BiliComment> replies;
        if (root == 0) {
            //已改用新的main api，旧版翻页api已被和谐
            MainApiCommentPage page = biliApiService
                    .getCommentMainPage(getBuvid3Cookie(account.cookie), oid, type,
                            2, PaginationStr.INITIAL, null)
                    .success("获取评论列表失败");
            replies = page.replies;
            //置顶评论要考虑
            if (page.top_replies != null) {
                for (BiliComment topReply : page.top_replies) {
                    if (topReply.rpid == rpid) {
                        return topReply;
                    }
                }
            }
            //不使用where(true)死循环，避免意外情况导致死循环调用api
            for (int i = 0; i < 30; i++) {
                if (replies != null && replies.size() > 0) {
                    for (BiliComment reply : replies) {
                        if (reply.rpid == rpid) {
                            return reply;
                        }
                        //到特定时间戳截止
                        if (reply.ctime < (sentTime.getTime() / 1000)) {
                            System.out.printf("到达评论 『%s』 其发送日期 %s < %s 终止查找\n",
                                    CommentUtil.omitComment(reply.content.message, 50),
                                    reply.ctime, sentTime.getTime() / 1000);
                            return null;
                        }
                    }
                }
                if (page.cursor.pagination_reply != null && page.cursor.pagination_reply.next_offset != null) {
                    page = biliApiService.getCommentMainPage(getBuvid3Cookie(account.cookie), oid, type,
                            2, new PaginationStr(page.cursor.pagination_reply.next_offset).toJson(), null).data();
                } else {
                    return null;
                }
            }
            throw new IOException("啊？！翻页超过30页了，你所发布的评论是否太久远了？或者程序因特殊原因陷入了死循环");
        } else {
            //楼中楼评论
            return findCommentFromCommentReplyArea(oid, type, rpid, root, account, false);
        }
    }

    public BiliComment findCommentFromCommentReplyArea(Comment comment, Account account, boolean isLogin) throws BiliBiliApiException, IOException {
        return findCommentFromCommentReplyArea(comment.commentArea.oid, comment.commentArea.type, comment.rpid, comment.root, account, isLogin);
    }

    public BiliComment findCommentFromCommentReplyArea(long oid, int type, long rpid, long root, Account account, boolean isLogin) throws IOException, BiliBiliApiException {
        assert root != 0;
        String cookie = account.cookie;
        GeneralResponse<MainApiCommentPage> body;
        if (isLogin) {
            body = biliApiService.getCommentMainPage(cookie, oid, type,
                    BiliApiService.COMMENT_SORT_MODE_TIME, PaginationStr.INITIAL, rpid).exe();
        } else {
            body = biliApiService.getCommentMainPage(getBuvid3Cookie(cookie), oid,
                    type, BiliApiService.COMMENT_SORT_MODE_TIME,
                    PaginationStr.INITIAL, rpid).exe();
        }
        if (!body.isSuccess()) {
            throw new BiliBiliApiException(body, "根评论被删除或ShadowBan等，无法获取回复列表");
        }
        List<BiliComment> comments = new ArrayList<>(body.data.replies);
        //有可能被顶置，所以把这个弄一起
        if (body.data.top_replies != null) {
            comments.addAll(body.data.top_replies);
        }
        //遍历根评论列表
        for (BiliComment comment : comments) {
            if (comment.rpid == root) {
                //遍历评论回复预览列表
                List<BiliComment> replies = comment.replies;
                if (replies != null) {
                    for (BiliComment reply : replies) {
                        if (reply.rpid == rpid) {
                            return reply;
                        }
                    }
                }
            }
        }
        return null;
    }

    public BiliComment findCommentUsingSeekRpid(Comment comment, Account account, boolean hasAccount) throws IOException, BiliBiliApiException {
        System.out.println("我调用了");
        String cookie = account.cookie;
        CommentArea commentArea = comment.commentArea;
        GeneralResponse<MainApiCommentPage> body;
        if (hasAccount) {
            body = biliApiService.getCommentMainPage(cookie, commentArea.oid,
                    commentArea.type, BiliApiService.COMMENT_SORT_MODE_TIME,
                    PaginationStr.INITIAL, comment.rpid).exe();
        } else {
            body = biliApiService.getCommentMainPage(getBuvid3Cookie(cookie), commentArea.oid,
                    commentArea.type, BiliApiService.COMMENT_SORT_MODE_TIME,
                    PaginationStr.INITIAL, comment.rpid).exe();
        }

        if (body.isSuccess()) {
            List<BiliComment> comments = body.data.replies;
            if (comments == null || comments.size() == 0) {
                return null;
            }

            for (BiliComment gotAComment : comments) {
                if (gotAComment.rpid == comment.rpid) {
                    return gotAComment;
                }
                if (gotAComment.replies != null){
                    for (BiliComment reply : gotAComment.replies) {
                        if (reply.rpid == comment.rpid){
                            return reply;
                        }
                    }
                }
            }

            //评论被置顶的情况
            List<BiliComment> topReplies = body.data.top_replies;
            if (topReplies == null || topReplies.size() == 0) {
                return null;
            }
            for (BiliComment aTopComment : topReplies) {
                if (aTopComment.rpid == comment.rpid) {
                    return aTopComment;
                }
            }
            return null;
        } else {
            throw new BiliBiliApiException(body, "获取评论列表(/reply/main)时发生错误");
        }
    }

    public interface PageTurnListener {
        void onPageTurn(int page);
    }


    public CommentArea matchCommentArea(String input, Account account) throws IOException, BiliBiliApiException {
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

        if (input.startsWith("https://www.bilibili.com/video/BV") || input.startsWith("https://m.bilibili.com/video/BV")
                || input.startsWith("http://www.bilibili.com/video/BV") || input.startsWith("http://m.bilibili.com/video/BV")) {
            String sourceId = subUrl(input, "/video/", 12);
            if (bvidToOid(sourceId) != null) {
                return new CommentArea(Long.parseLong(bvidToOid(sourceId)), sourceId, CommentArea.AREA_TYPE_VIDEO);
            } else {
                return null;
            }
        } else if (input.startsWith("https://www.bilibili.com/video/av") || input.startsWith("https://m.bilibili.com/video/av")
                || input.startsWith("http://www.bilibili.com/video/BV") || input.startsWith("http://m.bilibili.com/video/BV")) {
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
            return dvidToCommentArea(subUrl(input, "t.bilibili.com/", 18), account);
        } else if (input.startsWith("https://m.bilibili.com/opus/")) {
            String sourceId = subUrl(input, "/opus/", 18);
            return dvidToCommentArea(sourceId, account);
        } else if (input.startsWith("https://m.bilibili.com/dynamic/")) {
            String sourceId = subUrl(input, "/dynamic/", 18);
            return dvidToCommentArea(sourceId, account);
        }
        return null;
    }

    public void matchCommentAreaInUi(String input, Account account, MatchCommentAreaCallBack callBack) {
        TaskManger.start(() -> {
            try {
                CommentArea commentArea = matchCommentArea(input, account);
                TaskManger.postOnUiThread(() -> callBack.onMatchedArea(commentArea));
            } catch (IOException e) {
                TaskManger.postOnUiThread(() -> callBack.onNetworkError(e));
            } catch (BiliBiliApiException e) {
                TaskManger.postOnUiThread(() -> callBack.onApiError(e));
            }
        });
    }

    public interface MatchCommentAreaCallBack {
        void onNetworkError(IOException e);

        void onMatchedArea(CommentArea commentArea);

        void onApiError(BiliBiliApiException e);
    }

    private String subUrl(String url, String text, int length) {
        String subText = url.substring(url.indexOf(text) + text.length(), url.indexOf(text) + text.length() + length);
        System.out.println(subText);
        return subText;
    }

    /**
     * 获取戒严评论区信息。调用方法前请勿删除测试评论
     *
     * @param commentArea
     * @param testCommentRpid
     * @return
     * @throws IOException
     */
    public MartialLawCommentArea getMartialLawCommentArea(CommentArea commentArea, long testCommentRpid, Account account) throws IOException {
        byte[] coverImageData = null;
        String title = null, up = null;
        GeneralResponse<CommentReplyPage> resp = getCommentReplyHasAccount(commentArea, testCommentRpid, 1, account);
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
                    .addHeader("Referer", "https://t.bilibili.com/")
                    .addHeader("user-agent", "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + " Build/" + Build.ID + ") " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.163 Mobile Safari/537.36")
                    .addHeader("Cookie", account.cookie)
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (response.code() == 200) {
                JSONObject respJson = JSON.parseObject(response.body().string());
                if (respJson.getInteger("code") == 0) {
                    up = respJson.getJSONObject("data")
                            .getJSONObject("item")
                            .getJSONObject("modules")
                            .getJSONObject("module_author")
                            .getString("name");
                    title = respJson.getJSONObject("data")
                            .getJSONObject("item")
                            .getJSONObject("modules")
                            .getJSONObject("module_dynamic")
                            .getJSONObject("desc")
                            .getString("text");
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
        if (resp.code == GeneralResponse.CODE_COMMENT_DELETED) {
            defaultDisposalMethod = MartialLawCommentArea.DISPOSAL_METHOD_QUICK_DELETE;
        } else {
            defaultDisposalMethod = MartialLawCommentArea.DISPOSAL_METHOD_SHADOW_BAN;
        }
        return new MartialLawCommentArea(commentArea, defaultDisposalMethod, title, up, coverImageData);
    }

    public GeneralResponse<CommentAppealResp> appealComment(String id, String reason, Account account) throws IOException {
        String cookie = account.cookie;
        String idType = "oid";
        if (id.startsWith("http")) {
            idType = "url";
        }
        Map<String, String> map = new HashMap<>();
        map.put("csrf", getCsrfFromCookie(cookie));
        map.put(idType, id);
        //type不是评论区类型，而是申诉类型[评论申诉,图文动态申诉]
        map.put("type", "1");
        map.put("reason", reason);
        return biliApiService.appealComment(cookie, map).exe();
    }

    public void deleteComment(CommentArea commentArea, long rpid, Account account) throws IOException, BiliBiliApiException {
        String cookie = account.cookie;
        biliApiService.deleteComment(cookie, getCsrfFromCookie(cookie), commentArea.oid, commentArea.type, rpid)
                .success(String.format("[rpid=%s][cookie:uid=%s]评论删除失败！", rpid, account.uid));
    }

    public GeneralResponse<CommentReplyPage> getCommentReplyNoAccount(CommentArea commentArea, long rootRpid, int pn) throws IOException {
        return biliApiService.getCommentReply(commentArea.oid, pn, 20, rootRpid, commentArea.type, 0).exe();
    }

    public GeneralResponse<CommentReplyPage> getCommentReplyHasAccount(CommentArea commentArea, long rootRpid, int pn, Account account) throws IOException {
        String cookie = account.cookie;
        return biliApiService.getCommentReply(cookie, getCsrfFromCookie(cookie), commentArea.oid, pn, 20, rootRpid, commentArea.type, 0).exe();
    }


    public ForwardDynamicResult forwardDynamic(@NonNull String dynamicId, Account account) throws IOException, BiliBiliApiException {
        String cookie = account.cookie;
        return biliApiService.forwardDynamic(
                        cookie,
                        "web",
                        getCsrfFromCookie(cookie),
                        ForwardDynamicReqObject.create(account.uid, dynamicId))
                .success("转发动态失败");
    }

    public void deleteDynamic(@NonNull String dynamicId, Account account) throws IOException, BiliBiliApiException {
        String cookie = account.cookie;
        biliApiService.removeDynamic
                        (cookie, "web", getCsrfFromCookie(cookie), new RemoveDynamicReqObject(dynamicId))
                .success("删除动态 " + dynamicId + " 失败");
    }

    public boolean checkCookieNotFailed(Account account) throws IOException {
        Request request = new Request.Builder()
                .url("https://member.bilibili.com/x2/creative/h5/calendar/event?ts=0")
                .addHeader("Cookie", account.cookie)
                .build();
        ResponseBody body = httpClient.newCall(request).execute().body();
        OkHttpUtil.respNotNull(body);
        JSONObject userProfileJSON = JSON.parseObject(body.string());
        JSONObject userProfile = userProfileJSON.getJSONObject("data").getJSONObject("pfs");
        return userProfile != null;
    }

    public String getBuvid3Cookie(String cookie) {
        String patternString = "buvid3=[^;]+";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(cookie);
        if (!matcher.find()){
            throw new IllegalArgumentException("Cookie不完整！未找到buvid3字段，可尝试关闭使用B站客户端cookie，然后手动网页登录获取Cookie。" +
                    "若你使用的是第三方B站客户端，请将此信息发给那个客户端的开发者");
        }
        return matcher.group();
    }

    /**
     * 快速检测根评论（根评论）状态
     *
     * @param historyComment
     * @param account
     * @return 更新状态后的历史评论，为null为评论区失效
     * @throws BiliBiliApiException
     * @throws IOException
     */
    public HistoryComment recheckRootCommentStateByFast(HistoryComment historyComment, Account account) throws BiliBiliApiException, IOException {
        assert historyComment.root == 0;
        CommentArea commentArea = historyComment.commentArea;
        long rpid = historyComment.rpid;
        GeneralResponse<CommentReplyPage> grH = getCommentReplyHasAccount(commentArea, rpid, 1, account);
        if (grH.isSuccess()) {
            BiliComment rootComment = grH.data.root;
            GeneralResponse<CommentReplyPage> grN = getCommentReplyNoAccount(commentArea, rpid, 1);
            if (grN.isSuccess()) {
                BiliComment foundComment = findCommentUsingSeekRpid(historyComment, account, false);
                if (foundComment == null) {
                    //评论疑似审核中
                    return updateHistoryComment(null, HistoryComment.STATE_UNDER_REVIEW, historyComment);
                } else {
                    if (rootComment.invisible) {
                        //评论invisible
                        return updateHistoryComment(foundComment, HistoryComment.STATE_INVISIBLE, historyComment);
                    } else {
                        //评论正常
                        return updateHistoryComment(foundComment, HistoryComment.STATE_NORMAL, historyComment);
                    }
                }
            } else if (grN.code == GeneralResponse.CODE_COMMENT_DELETED) {
                return updateHistoryComment(rootComment, HistoryComment.STATE_SHADOW_BAN, historyComment);
            } else {
                throw new BiliBiliApiException(grN, "无账号获取评论回复页失败");
            }
        } else if (grH.code == GeneralResponse.CODE_COMMENT_DELETED) {
            //有账号获取还提示已删除就是真删
            return updateHistoryComment(null, HistoryComment.STATE_DELETED, historyComment);
        } else if (grH.code == GeneralResponse.CODE_COMMENT_AREA_CLOSED) {
            return null;
        } else {
            throw new BiliBiliApiException(grH, "有获取评论回复页失败");
        }
    }

    public HistoryComment recheckReplyCommentState(HistoryComment historyComment, Account account) throws BiliBiliApiException, IOException, RootCommentDeadException {
        GeneralResponse<CommentReplyPage> gr = getCommentReplyNoAccount(historyComment.commentArea, historyComment.root, 1);
        if (gr.isSuccess()) {
            //不登录seek_rpid查找评论
            BiliComment foundReply = findCommentFromCommentReplyArea(historyComment, account, false);
            if (foundReply != null) {
                if (foundReply.invisible) {
                    //回复评论invisible
                    return updateHistoryComment(foundReply, HistoryComment.STATE_INVISIBLE, historyComment);
                } else {
                    //回复评论正常
                    return updateHistoryComment(foundReply, HistoryComment.STATE_NORMAL, historyComment);
                }
            } else {
                //登录seek_rpid查找评论
                BiliComment foundReplyHasAcc = findCommentFromCommentReplyArea(historyComment, account, true);
                if (foundReplyHasAcc != null) {
                    //回复评论ShadowBan
                    return updateHistoryComment(foundReplyHasAcc, HistoryComment.STATE_SHADOW_BAN, historyComment);
                } else {
                    //回复评论被删除
                    return updateHistoryComment(null, HistoryComment.STATE_DELETED, historyComment);
                }
            }
        } else if (gr.code == GeneralResponse.CODE_COMMENT_DELETED) {//根评论挂了
            throw new RootCommentDeadException(historyComment.root, gr);
        } else if (gr.code == GeneralResponse.CODE_COMMENT_AREA_CLOSED) {
            return null;
        } else {
            throw new BiliBiliApiException(gr, "获取评论回复页失败");
        }
    }

    public static HistoryComment updateHistoryComment(BiliComment biliComment, String state, HistoryComment historyComment) {
        //当前面申诉提示无评论可申诉时，后面再检测到疑似审核就不改变状态
        historyComment.lastCheckDate = new Date();
        if (!(historyComment.lastState.equals(HistoryComment.STATE_SUSPECTED_NO_PROBLEM) && state.equals(HistoryComment.STATE_UNDER_REVIEW))) {
            historyComment.lastState = state;
        }
        if (biliComment != null) {
            historyComment.like = biliComment.like;
            historyComment.replyCount = biliComment.rcount;
        }
        return historyComment;
    }

    public static class RootCommentDeadException extends Throwable {
        public final long rootRpid;
        public final GeneralResponse<?> generalResponse;

        public RootCommentDeadException(long root, GeneralResponse<?> generalResponse) {
            this.rootRpid = root;
            this.generalResponse = generalResponse;
        }
    }

    /*public String getDedeUserID(boolean isDeputy){
        String patternString = "DedeUserID=([^;]+)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(isDeputy ? deputyCookie : cookie);
        matcher.find();
        return matcher.group(1);

    }*/
}
