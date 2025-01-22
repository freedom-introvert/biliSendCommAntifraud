package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.concurrent.atomic.AtomicInteger;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class CommentLocator {
    public static void lunch(Context context,int areaType,long oid,long rpid,long root,String sourceId) {
        AtomicInteger selected = new AtomicInteger(0);
        new AlertDialog.Builder(context)
                .setTitle("选择b站版本（需要挂载XP/LSP）")
                .setSingleChoiceItems(new String[]{"国内版" ,"国际版"}, 0, (dialog, which) -> selected.set(which))
                .setNegativeButton(R.string.cancel,null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (selected.get() == 0){
                        lunchMaster(context, areaType, oid, rpid, root, sourceId);
                    } else {
                        lunchGlobal(context, areaType, oid, rpid, root, sourceId);
                    }
                })
                .show();

    }

    public static void lunchMaster(Context context,int areaType,long oid,long rpid,long root,String sourceId){
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        Bundle extras = new Bundle();
        if (areaType == CommentArea.AREA_TYPE_VIDEO) {
            intent.setClassName("tv.danmaku.bili", "tv.danmaku.bili.MainActivityV2");
            intent.putExtra("TransferActivity", "com.bilibili.video.videodetail.VideoDetailsActivity");
            extras.putString("id", String.valueOf(oid));

            //根评论与评论回复的不同处理方法
            if (root != 0) {
                extras.putString("comment_root_id", String.valueOf(root));
                extras.putString("comment_secondary_id", String.valueOf(rpid));
            } else {
                extras.putString("comment_root_id", String.valueOf(rpid));
            }
            extras.putString("comment_from_spmid", "im.notify-reply.0.0");
            extras.putString("tab_index", "1");
            intent.putExtra("transferUri", "bilibili://video/" + oid);
        } else if (areaType == CommentArea.AREA_TYPE_DYNAMIC11 || areaType == CommentArea.AREA_TYPE_DYNAMIC17) {
            intent.setClassName("tv.danmaku.bili", "tv.danmaku.bili.MainActivityV2");
            intent.putExtra("TransferActivity", "com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity");
            if (root != 0) {
                extras.putString("commentId", String.valueOf(root));
            } else {
                extras.putString("commentId", String.valueOf(rpid));
            }
            extras.putString("anchor", String.valueOf(rpid));
            extras.putString("oid", String.valueOf(oid));
            extras.putString("type", String.valueOf(areaType));
            extras.putString("enterUri", "bilibili://following/detail/" + sourceId);
            extras.putString("comment_from_spmid", "im.notify-reply.0.0");
            extras.putString("enterName", "查看动态详情");
            extras.putString("showEnter", "1");
        } else if (areaType == CommentArea.AREA_TYPE_ARTICLE) {
            intent.setClassName("tv.danmaku.bili", "tv.danmaku.bili.MainActivityV2");
            intent.putExtra("TransferActivity", "com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity");
            if (root != 0) {
                extras.putString("commentId", String.valueOf(root));
            } else {
                extras.putString("commentId", String.valueOf(rpid));
            }
            extras.putString("anchor", String.valueOf(rpid));
            extras.putString("oid", String.valueOf(oid));
            extras.putString("type", String.valueOf(areaType));
            extras.putString("enterUri", "bilibili://article/" + oid);
            extras.putString("comment_from_spmid", "im.notify-reply.0.0");
            extras.putString("enterName", "查看文章详情");
            extras.putString("showEnter", "1");
        }
        intent.putExtra("TransferExtras", extras);
        context.startActivity(intent);
    }

    public static void lunchGlobal(Context context,int areaType,long oid,long rpid,long root,String sourceId){
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        Bundle extras = new Bundle();
        if (areaType == CommentArea.AREA_TYPE_VIDEO) {
            intent.setClassName("com.bilibili.app.in", "tv.danmaku.bili.MainActivityV2");
            intent.putExtra("TransferActivity", "com.bilibili.video.videodetail.VideoDetailsActivity");
            extras.putString("id", String.valueOf(oid));

            //根评论与评论回复的不同处理方法
            if (root != 0) {
                extras.putString("comment_root_id", String.valueOf(root));
                extras.putString("comment_secondary_id", String.valueOf(rpid));
            } else {
                extras.putString("comment_root_id", String.valueOf(rpid));
            }
            extras.putString("comment_from_spmid", "im.notify-reply.0.0");
            extras.putString("tab_index", "1");
            intent.putExtra("transferUri", "bilibili://video/" + oid);
        } else if (areaType == CommentArea.AREA_TYPE_DYNAMIC11 || areaType == CommentArea.AREA_TYPE_DYNAMIC17) {
            intent.setClassName("com.bilibili.app.in", "tv.danmaku.bili.MainActivityV2");
            intent.putExtra("TransferActivity", "com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity");
            if (root != 0) {
                extras.putString("commentId", String.valueOf(root));
            } else {
                extras.putString("commentId", String.valueOf(rpid));
            }
            extras.putString("anchor", String.valueOf(rpid));
            extras.putString("oid", String.valueOf(oid));
            extras.putString("type", String.valueOf(areaType));
            extras.putString("enterUri", "bilibili://following/detail/" + sourceId);
            extras.putString("comment_from_spmid", "im.notify-reply.0.0");
            extras.putString("enterName", "查看动态详情");
            extras.putString("showEnter", "1");
        } else if (areaType == CommentArea.AREA_TYPE_ARTICLE) {
            intent.setClassName("com.bilibili.app.in", "tv.danmaku.bili.MainActivityV2");
            intent.putExtra("TransferActivity", "com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity");
            if (root != 0) {
                extras.putString("commentId", String.valueOf(root));
            } else {
                extras.putString("commentId", String.valueOf(rpid));
            }
            extras.putString("anchor", String.valueOf(rpid));
            extras.putString("oid", String.valueOf(oid));
            extras.putString("type", String.valueOf(areaType));
            extras.putString("enterUri", "bilibili://article/" + oid);
            extras.putString("comment_from_spmid", "im.notify-reply.0.0");
            extras.putString("enterName", "查看文章详情");
            extras.putString("showEnter", "1");
        }
        intent.putExtra("TransferExtras", extras);
        context.startActivity(intent);
    }
}
