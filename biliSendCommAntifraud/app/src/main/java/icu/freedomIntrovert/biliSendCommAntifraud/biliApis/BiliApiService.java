package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BiliApiService {
    @GET("/x/web-interface/view")
    Call<GeneralResponse<VideoInfo>> getVideoInfoByAid(@Query("aid") long aid);
    @GET("/x/web-interface/view")
    Call<GeneralResponse<VideoInfo>> getVideoInfoByBvid(@Query("bvid") String bvid);
    @FormUrlEncoded
    @POST("/x/v2/reply/add")
    Call<GeneralResponse<CommentAddResult>> postComment(@Header ("cookie") String cookie,@FieldMap Map<String, String> map);
    @FormUrlEncoded
    @POST("/x/v2/reply/del")
    Call<Void> deleteComment(@Header ("cookie") String cookie,@Field("csrf") String csrf, @Field("oid") long oid, @Field("type") int type, @Field("rpid") long rpid);
    @GET("/x/v2/reply/reply")
    Call<GeneralResponse<CommentReply>> getCommentReply(@Header ("cookie") String cookie,@Query("csrf")String csrf,@Query("oid") long oid,@Query("pn") int pn,@Query("ps") int ps,@Query("root") long root,@Query("type") int type);//csrf=a8bd67d9496b74f2b001b1e0529de4f9&oid=655062494&pn=1&ps=10&root=161192377344&type=1)
    @GET("/x/v2/reply/reply")
    Call<GeneralResponse<CommentReply>> getCommentReply(@Query("oid") long oid,@Query("pn") int pn,@Query("ps") int ps,@Query("root") long root,@Query("type") int type);

}
