package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.CommentReviewPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import okhttp3.OkHttpClient;

public class BannedCommentListAdapter extends RecyclerView.Adapter<BannedCommentListAdapter.ViewHolder> {

    ArrayList<BannedCommentBean> bandCommentBeanArrayList;
    Context context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    CommentReviewPresenter commentReviewPresenter;
    Config config;

    public BannedCommentListAdapter(ArrayList<BannedCommentBean> bandCommentBeanArrayList, Context context) {
        this.bandCommentBeanArrayList = bandCommentBeanArrayList;
        Collections.reverse(this.bandCommentBeanArrayList);
        this.context = context;
        config = new Config(context);
        CommentManipulator commentManipulator = new CommentManipulator(new OkHttpClient(), config.getCookie(),config.getDeputyCookie());
        commentReviewPresenter = new CommentReviewPresenter(new Handler(), commentManipulator);
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.item_banned_comment, null);
        return new ViewHolder(view);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BannedCommentBean bannedCommentBean = bandCommentBeanArrayList.get(position);

        holder.txv_info.setText(bannedCommentBean.commentArea.sourceId);
        switch (bannedCommentBean.checkedArea) {
            case BannedCommentBean.CHECKED_NO_CHECK:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.GRAY));
                break;
            case BannedCommentBean.CHECKED_NOT_MARTIAL_LAW:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.blue));
                break;
            case BannedCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.red));
                break;
            case BannedCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.green));
                break;
        }
        holder.txv_comment.setText(bannedCommentBean.comment);
        switch (bannedCommentBean.bannedType) {
            case BannedCommentBean.BANNED_TYPE_SHADOW_BAN:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.hide));
                holder.txv_banned_type.setText("仅自己可见");
                break;
            case BannedCommentBean.BANNED_TYPE_SHADOW_BAN_RECKONING:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.hide));
                holder.txv_banned_type.setText("仅自己可见(秋后算账)");
                break;
            case BannedCommentBean.BANNED_TYPE_UNDER_REVIEW:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.hide));
                holder.txv_banned_type.setText("疑似审核中");
                break;
            case BannedCommentBean.BANNED_TYPE_QUICK_DELETE:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.deleted));
                holder.txv_banned_type.setText("被系统秒删");
                break;
            case BannedCommentBean.BANNED_TYPE_SENSITIVE:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.sensitive));
                holder.txv_banned_type.setText("包含敏感词");
                break;
            case BannedCommentBean.BANNED_TYPE_INVISIBLE:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.ghost));
                holder.txv_banned_type.setText("评论被隐身");
                break;
            case BannedCommentBean.BANNED_TYPE_UNKNOWN:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.unknown));
                holder.txv_banned_type.setText("未知");
                break;
            case BannedCommentBean.BANNED_TYPE_SUSPECTED_NO_PROBLEM:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_access_time_24));
                holder.txv_banned_type.setText("评论疑似正常");
                break;
        }
        holder.txv_date.setText(bannedCommentBean.getFormatDateFor_yMd());
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });

        holder.itemView.setOnClickListener(v -> {
            View dialogView = View.inflate(context, R.layout.dialog_banned_comment_info, null);
            TextView txv_comment_content = dialogView.findViewById(R.id.txv_comment_content);
            TextView txv_copy_comment = dialogView.findViewById(R.id.txv_copy_comment);
            TextView txv_oid = dialogView.findViewById(R.id.txv_oid);
            TextView txv_source_id = dialogView.findViewById(R.id.txv_source_id);
            TextView txv_band_type = dialogView.findViewById(R.id.txv_band_type);
            TextView txv_area_type = dialogView.findViewById(R.id.txv_area_type);
            TextView txv_send_date = dialogView.findViewById(R.id.txv_send_date);
            TextView txv_checkedArea = dialogView.findViewById(R.id.txv_checked_area);
            txv_comment_content.setText(bannedCommentBean.comment);
            txv_copy_comment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData mClipData = ClipData.newPlainText("Label", bannedCommentBean.comment);
                    cm.setPrimaryClip(mClipData);
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
                }
            });
            txv_oid.setText(String.valueOf(bannedCommentBean.commentArea.oid));
            txv_source_id.setText(bannedCommentBean.commentArea.sourceId);
            switch (bannedCommentBean.bannedType) {
                case BannedCommentBean.BANNED_TYPE_SHADOW_BAN:
                    txv_band_type.setText("仅自己可见");
                    break;
                case BannedCommentBean.BANNED_TYPE_SHADOW_BAN_RECKONING:
                    txv_band_type.setText("仅自己可见(秋后算账)");
                    break;
                case BannedCommentBean.BANNED_TYPE_QUICK_DELETE:
                    txv_band_type.setText("被系统秒删");
                    break;
                case BannedCommentBean.BANNED_TYPE_UNDER_REVIEW:
                    txv_band_type.setText("疑似审核中");
                    break;
                case BannedCommentBean.BANNED_TYPE_SENSITIVE:
                    txv_band_type.setText("包含敏感词");
                    break;
                case BannedCommentBean.BANNED_TYPE_INVISIBLE:
                    txv_band_type.setText("评论被隐身(可获取到，但前端不展示，因为属性：invisible=true)");
                    break;
                case BannedCommentBean.BANNED_TYPE_UNKNOWN:
                    txv_band_type.setText("未知（直接去申诉等无法得知具体状态）");
                    break;
                case BannedCommentBean.BANNED_TYPE_SUSPECTED_NO_PROBLEM:
                    txv_band_type.setText("评论疑似正常，因为申诉时提示无可申诉评论（可能等待时间设置太短所以误判导致，或处于某种处理或审核状态，等待一段时间后应该可以显示）");
                    break;
            }
            txv_area_type.setText(bannedCommentBean.commentArea.getAreaTypeDesc());
            switch (bannedCommentBean.checkedArea) {
                case BannedCommentBean.CHECKED_NO_CHECK:
                    txv_checkedArea.setText("未检查");
                    break;
                case BannedCommentBean.CHECKED_NOT_MARTIAL_LAW:
                    txv_checkedArea.setText("只检查过未戒严");
                    break;
                case BannedCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA:
                    txv_checkedArea.setText("仅在在此评论区被ban");
                    break;
                case BannedCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA:
                    txv_checkedArea.setText("评论区一切正常，该评论在任何评论区都被ban");
                    break;
            }


            txv_send_date.setText(bannedCommentBean.getFormatDateFor_yMdHms());

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("评论详情")
                    .setView(dialogView)
                    .setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener())
                    .setNegativeButton("复查状态", (dialog13, which) -> {
                        ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, null, "复查中……");
                        progressDialog.show();
                        commentReviewPresenter.reviewStatus(bannedCommentBean.commentArea, bannedCommentBean.rpid, new CommentReviewPresenter.ReviewStatusCallBack() {
                            @Override
                            public void deleted() {
                                progressDialog.dismiss();
                                DialogUtil.dialogMessage(context,"检查结果","评论被删除！");
                            }

                            @Override
                            public void shadowBanned(int like,int rcount) {
                                DialogUtil.dialogMessage(context,"检查结果","评论处于shadowBan状态");
                                progressDialog.dismiss();
                            }

                            @Override
                            public void ok(int like,int rcount) {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(context)
                                        .setTitle("检查结果")
                                        .setMessage("评论正常，是否继续翻遍评论区？")
                                        .setNegativeButton("取消",new VoidDialogInterfaceOnClickListener())
                                        .setPositiveButton("继续", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                searchThroughoutTheCommentArea(bannedCommentBean.commentArea, bannedCommentBean.rpid, holder);
                                            }
                                        })
                                        .show();
                            }

                            @Override
                            public void onPageTurnForNoAccReply(int pn) {
                                progressDialog.setMessage("正在无账号条件下查找评论回复列表，第"+pn+"页");
                            }

                            @Override
                            public void onPageTurnForHasAccReply(int pn) {
                                progressDialog.setMessage("正在有账号条件下查找评论回复列表，第"+pn+"页");
                            }

                            @Override
                            public void replyOk(int like, int replyCount) {
                                progressDialog.dismiss();
                                DialogUtil.dialogMessage(context,"检查结果","此回复评论正常显示！");
                            }

                            @Override
                            public void rootCommentIsShadowBan() {
                                progressDialog.dismiss();
                                DialogUtil.dialogMessage(context,"检查结果","你的根评论后期遭到shadowBan，此条回复评论被连累了！");
                            }

                            @Override
                            public void invisible(int like, int replyCount) {
                                progressDialog.dismiss();
                                DialogUtil.dialogMessage(context,"检查结果","评论invisible，前端不可见！");
                            }

                            @Override
                            public void onCodeError(int code, String message) {
                                progressDialog.dismiss();
                                DialogUtil.dialogMessage(context,"评论区异常","code="+code+"\nmessage="+message);
                            }

                            @Override
                            public void onNetworkError(Throwable th) {
                                progressDialog.dismiss();
                                DialogUtil.dialogMessage(context,"网络错误",th.getLocalizedMessage());
                            }
                        });
                    })
                    //.setNegativeButton("复制", (dialog13, which) -> {
                    //    ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    //    ClipData mClipData = ClipData.newPlainText("Label", bandCommentBean.comment);
                    //    cm.setPrimaryClip(mClipData);
                    //    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
                    //})
                    .setNeutralButton("删除", (dialog12, which) -> {
                        new AlertDialog.Builder(context).setMessage("确认删除吗？")
                                .setNegativeButton("手滑了", new VoidDialogInterfaceOnClickListener())
                                .setPositiveButton("确认", (dialog14, which2) -> {
                                    if (statisticsDBOpenHelper.deleteBannedComment(bannedCommentBean.rpid) != 0) {
                                        bandCommentBeanArrayList.remove(holder.getAdapterPosition());
                                        notifyItemRemoved(holder.getAdapterPosition());
                                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                                    }
                                }).show();
                    }).show();
        });
    }

    private void searchThroughoutTheCommentArea(CommentArea commentArea,long rpid, ViewHolder viewHolder){
        ProgressDialog progressDialog = DialogUtil.newProgressDialog(context,"寻找中","准备……");
        progressDialog.show();
        commentReviewPresenter.searchThroughoutTheCommentArea(commentArea, rpid, new CommentReviewPresenter.SearchTTCommAreaCallback() {
            @Override
            public void onPageTurn(int pn) {
                progressDialog.setMessage("正在无账号条件下查找评论列表，第"+pn+"页");
            }

            @Override
            public void found() {
                progressDialog.dismiss();
                DialogUtil.dialogMessage(context,"寻找结果","无账号下找到了你的评论，该评论正常显示！");
            }

            @Override
            public void notFound() {
                progressDialog.dismiss();
                notifyItemChanged(viewHolder.getAdapterPosition());
                DialogUtil.dialogMessage(context,"寻找评论","无账号下翻遍了评论区，未找到你的评论！评论审核中或ShadowBan+");
            }

            @Override
            public void onNetworkError(Throwable th) {
                progressDialog.dismiss();
                DialogUtil.dialogMessage(context,"网络错误",th.getLocalizedMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        return bandCommentBeanArrayList.size();
    }

    public void addData(List<BannedCommentBean> bannedCommentBeans) {
        Collections.reverse(bannedCommentBeans);
        bandCommentBeanArrayList.addAll(0, bannedCommentBeans);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        TextView txv_comment, txv_banned_type, txv_info, txv_date;

        ImageView imgv_banned_type;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            txv_comment = itemView.findViewById(R.id.txv_comment_content);
            txv_banned_type = itemView.findViewById(R.id.txv_band_type);
            txv_info = itemView.findViewById(R.id.txv_info);
            txv_date = itemView.findViewById(R.id.txv_date);
            imgv_banned_type = itemView.findViewById(R.id.img_band_type);
        }
    }
}
