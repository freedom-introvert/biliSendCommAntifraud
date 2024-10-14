package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import java.util.Map;

import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliCall;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BiliApiService {
    int COMMENT_SORT_BY_TIME = 0;
    int COMMENT_SORT_BY_LIKE = 1;
    int COMMENT_SORT_BY_REPLY_COUNT = 2;
    int COMMENT_SORT_MODE_HOT = 0;
    int COMMENT_SORT_MODE_TIME = 2;

    @GET("/x/web-interface/view")
    Call<GeneralResponse<VideoInfo>> getVideoInfoByAid(@Query("aid") long aid);

    @GET("/x/web-interface/view")
    BiliCall<GeneralResponse<VideoInfo>,VideoInfo> getVideoInfoByBvid(@Query("bvid") String bvid);

    @FormUrlEncoded
    @POST("/x/v2/reply/add")
    BiliCall<GeneralResponse<CommentAddResult>, CommentAddResult> postComment(@Header("cookie") String cookie, @FieldMap Map<String, String> map);

    @FormUrlEncoded
    @POST("/x/v2/reply/del")
    BiliCall<GeneralResponse<Object>, Object> deleteComment(@Header("cookie") String cookie, @Field("csrf") String csrf, @Field("oid") long oid, @Field("type") int type, @Field("rpid") long rpid);

    @GET("/x/v2/reply/reply")
    BiliCall<GeneralResponse<CommentReplyPage>, CommentReplyPage> getCommentReply(@Header("cookie") String cookie, @Query("csrf") String csrf, @Query("oid") long oid, @Query("pn") int pn, @Query("ps") int ps, @Query("root") long root, @Query("type") int type, @Query("sort") int sort);

    @GET("/x/v2/reply/reply")
    BiliCall<GeneralResponse<CommentReplyPage>,CommentReplyPage> getCommentReply
            (@Query("oid") long oid, @Query("pn") int pn, @Query("ps") int ps,
             @Query("root") long root, @Query("type") int type, @Query("sort") int sort);

    @GET("/x/v2/reply")
    Call<GeneralResponse<CommentPage>> getCommentPageNoAccount(@Query("oid") long oid, @Query("type") int type, @Query("pn") int pn, @Query("sort") int sort);

    @GET("/x/v2/reply")
    Call<GeneralResponse<CommentPage>> getCommentPageHasAccount(@Header("cookie") String cookie, @Query("csrf") String csrf, @Query("sort") int sort, @Query("oid") long oid, @Query("pn") int pn, @Query("type") int type);

    /*@GET("/x/v2/reply/main")
    BBCall<GeneralResponse<CommentPage>, CommentPage> getCommentMainPage
    (@Header("cookie") String cookie, @Query("oid") long oid, @Query("type") int type,
     @Query("mode") int mode, @Query("next") int next, @Query("seek_rpid") long seek_rpid);*/


   /* @GET("/x/v2/reply/main")
    Call<GeneralResponse<CommentPage>> getCommentMainPageHasAccount
    (@Header("cookie") String cookie_buvid3, @Query("oid") long oid, @Query("type") int type,
     @Query("mode") int mode, @Query("next") int next, @Query("seek_rpid") long seek_rpid);*/

    /**
     * @param mode           排序方式
     *                       0 3：仅按热度
     *                       1：按热度+按时间
     *                       2：仅按时间
     * @param pagination_str 初始：{"offset":""}，然后从响应数据里取：cursor.pagination_reply.prev_offset 往前推进
     * @param cookie_buvid3  登录账号：完整cookie
     *                       不登录账号：buvid3=xxx 在cookie里任取。若不填会导致-352错误！
     *                       即使是从cookie获取的不会导致登录状态，因为无账号的cookie也有此参数
     * @param seek_rpid      定位评论的rpid，为null不进行定位。可使用楼中楼的rpid，定位评论将出现在预览域里
     * @return
     */
    @GET("/x/v2/reply/main")
    BiliCall<GeneralResponse<MainApiCommentPage>, MainApiCommentPage> getCommentMainPage
    (@Header("cookie") String cookie, @Query("oid") long oid, @Query("type") int type,
     @Query("mode") int mode, @Query("pagination_str") String pagination_str, @Query("seek_rpid") Long seek_rpid);

    @POST("/x/dynamic/feed/create/dyn")
    BiliCall<GeneralResponse<ForwardDynamicResult>, ForwardDynamicResult> forwardDynamic
            (@Header("cookie") String cookie, @Query("platform") String platform,
             @Query("csrf") String csrf, @Body ForwardDynamicReqObject forwardDynamicReqObject);

    @POST("/x/dynamic/feed/operate/remove")
    BiliCall<GeneralResponse<Object>, Object> removeDynamic
            (@Header("cookie") String cookie, @Query("platform") String platform,
             @Query("csrf") String csrf, @Body RemoveDynamicReqObject removeDynamicReqObject);

    /**
     * 获取账号信息
     *
     * @return
     */
    @GET("/x/web-interface/nav")
    BiliCall<GeneralResponse<Nav>, Nav> getNav(@Header("cookie") String cookie);

    @FormUrlEncoded
    @POST("/x/v2/reply/appeal/submit")
    BiliCall<GeneralResponse<CommentAppealResp>,CommentAppealResp> appealComment(@Header("cookie") String cookie, @FieldMap Map<String,String> map);

}
