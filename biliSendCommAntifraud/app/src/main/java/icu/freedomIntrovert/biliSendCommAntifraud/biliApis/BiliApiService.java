package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import java.util.Map;

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
    Call<GeneralResponse<VideoInfo>> getVideoInfoByBvid(@Query("bvid") String bvid);
    @FormUrlEncoded
    @POST("/x/v2/reply/add")
    Call<GeneralResponse<CommentAddResult>> postComment(@Header ("cookie") String cookie,@FieldMap Map<String, String> map);
    @FormUrlEncoded
    @POST("/x/v2/reply/del")
    Call<GeneralResponse<Object>> deleteComment(@Header ("cookie") String cookie,@Field("csrf") String csrf, @Field("oid") long oid, @Field("type") int type, @Field("rpid") long rpid);
    @GET("/x/v2/reply/reply")
    Call<GeneralResponse<CommentReplyPage>> getCommentReply(@Header ("cookie") String cookie, @Query("csrf")String csrf, @Query("oid") long oid, @Query("pn") int pn, @Query("ps") int ps, @Query("root") long root, @Query("type") int type, @Query("sort") int sort);
    @GET("/x/v2/reply/reply")
    Call<GeneralResponse<CommentReplyPage>> getCommentReply(@Query("oid") long oid, @Query("pn") int pn, @Query("ps") int ps, @Query("root") long root, @Query("type") int type, @Query("sort") int sort);
    @GET("/x/v2/reply")
    Call<GeneralResponse<CommentPage>> getCommentPageNoAccount(@Query("oid") long oid,@Query("type") int type,@Query("pn") int pn,@Query("sort") int sort);
    @GET("/x/v2/reply")
    Call<GeneralResponse<CommentPage>> getCommentPageHasAccount(@Header ("cookie") String cookie,@Query("csrf")String csrf,@Query("sort") int sort,@Query("oid") long oid,@Query("pn") int pn,@Query("type") int type);
    @GET("/x/v2/reply/main")
    Call<GeneralResponse<CommentPage>> getCommentMainPageHasAccount(@Header("cookie") String cookie,@Query("oid") long oid,@Query("type") int type,@Query("mode") int mode,@Query("next") int next,@Query("seek_rpid") long seek_rpid);
    /**
     *
     * @param cookie_buvid3 buvid3=xxx 在cookie里任取。若不填会导致-352错误！
     *                      从cookie获取的不会导致登录状态
     */
    @GET("/x/v2/reply/main")
    Call<GeneralResponse<CommentPage>> getCommentMainPageNoAccount(@Header("cookie") String cookie_buvid3,@Query("oid") long oid,@Query("type") int type,@Query("mode") int mode,@Query("next") int next,@Query("seek_rpid") long seek_rpid);

    @POST("/x/dynamic/feed/create/dyn")
    Call<GeneralResponse<ForwardDynamicResult>> forwardDynamic(@Header("cookie") String cookie,@Query("platform") String platform,@Query("csrf") String csrf,@Body ForwardDynamicReqObject forwardDynamicReqObject);

    @POST("/x/dynamic/feed/operate/remove")
    Call<GeneralResponse<Object>> removeDynamic(@Header("cookie") String cookie,@Query("platform") String platform,@Query("csrf") String csrf,@Body RemoveDynamicReqObject removeDynamicReqObject);
}
