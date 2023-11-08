package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.CommentPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.CommentReviewPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;
import okhttp3.OkHttpClient;

public class HistoryCommentAdapter extends RecyclerView.Adapter<HistoryCommentAdapter.ViewHolder> {
    Context context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    List<HistoryComment> historyCommentList;
    CommentReviewPresenter commentReviewPresenter;
    CommentPresenter commentPresenter;
    DialogCommCheckWorker dialogCommCheckWorker;

    public HistoryCommentAdapter(Context context,List<HistoryComment> historyCommentList,StatisticsDBOpenHelper statisticsDBOpenHelper) {
        this.context = context;
        this.statisticsDBOpenHelper = statisticsDBOpenHelper;
        this.historyCommentList = historyCommentList;
        Config config = new Config(context);
        Handler handler = new Handler();
        CommentManipulator commentManipulator = new CommentManipulator(new OkHttpClient(), config.getCookie(),config.getDeputyCookie());
        commentReviewPresenter = new CommentReviewPresenter(handler, commentManipulator);
        commentPresenter = new CommentPresenter(handler,commentManipulator,statisticsDBOpenHelper,config);
        this.dialogCommCheckWorker = new DialogCommCheckWorker(context, handler, commentManipulator, commentPresenter, new CommentUtil(config.sp_config), new OnExitListenerByComment() {
            @Override
            public void rpid(long rpid) {
                historyCommentList.add(0, statisticsDBOpenHelper.getHistoryComment(rpid));
            }

            @Override
            public void exit() {
                notifyDataSetChanged();
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = View.inflate(context,R.layout.item_history_comment,null);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryComment historyComment = historyCommentList.get(position);
        holder.txv_comment.setText(historyComment.comment);
        if (historyComment.state.equals(HistoryComment.STATE_DELETED)){
            holder.txv_comment.setTextColor(context.getResources().getColor(R.color.red));
            holder.txv_comment.getPaint().setFlags(Paint. STRIKE_THRU_TEXT_FLAG|Paint.ANTI_ALIAS_FLAG);
        } else if (historyComment.state.equals(HistoryComment.STATE_SHADOW_BAN)){
            holder.txv_comment.setTextColor(context.getResources().getColor(R.color.red));
            holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
        } else if (historyComment.state.equals(HistoryComment.STATE_INVISIBLE)){
            holder.txv_comment.setTextColor(context.getResources().getColor(R.color.gray));
            holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
        } else {
            holder.txv_comment.setTextColor(context.getResources().getColor(R.color.dark_font));
            holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
        }
        holder.txv_date.setText(historyComment.getFormatDateFor_yMd());
        holder.txv_info.setText(historyComment.commentArea.sourceId);
        holder.txv_like.setText(String.valueOf(historyComment.like));
        holder.txv_reply_count.setText(String.valueOf(historyComment.replyCount));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View dialogView = View.inflate(context,R.layout.dialog_history_comment_info,null);
                TextView txv_comment = dialogView.findViewById(R.id.txv_comment_content);
                TextView txv_last_state = dialogView.findViewById(R.id.txv_last_state);
                TextView txv_source_id = dialogView.findViewById(R.id.txv_source_id);
                TextView txv_oid = dialogView.findViewById(R.id.txv_oid);
                TextView txv_area_type = dialogView.findViewById(R.id.txv_area_type);
                TextView txv_rpid = dialogView.findViewById(R.id.txv_rpid);
                TextView txv_parent = dialogView.findViewById(R.id.txv_parent);
                TextView txv_root = dialogView.findViewById(R.id.txv_root);
                TextView txv_check_date = dialogView.findViewById(R.id.txv_check_date);
                TextView txv_send_date = dialogView.findViewById(R.id.txv_send_date);

                txv_comment.setText(historyComment.comment);
                txv_last_state.setText(historyComment.getStateDesc());
                txv_source_id.setText(historyComment.commentArea.sourceId);
                txv_oid.setText(String.valueOf(historyComment.commentArea.oid));
                txv_area_type.setText(historyComment.commentArea.getAreaTypeDesc());
                txv_rpid.setText(String.valueOf(historyComment.rpid));
                txv_parent.setText(String.valueOf(historyComment.parent));
                txv_root.setText(String.valueOf(historyComment.root));
                txv_check_date.setText(historyComment.getFormatLastCheckDateFor_yMdHms());
                txv_send_date.setText(historyComment.getFormatDateFor_yMdHms());
                new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton("关闭",new VoidDialogInterfaceOnClickListener())
                        .setNegativeButton("更新状态", (dialog, which) -> {
                            ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, null, "复查中……");
                            progressDialog.show();
                            commentReviewPresenter.reviewStatus(historyComment.commentArea, historyComment.rpid, new CommentReviewPresenter.ReviewStatusCallBack() {
                                @Override
                                public void onCookieFiled() {
                                    progressDialog.dismiss();
                                    DialogUtil.dialogMessage(context,"账号错误","您的cookie已过期，请重新获取！");
                                }

                                @Override
                                public void deleted() {
                                    progressDialog.dismiss();
                                    HistoryComment historyComment1 = historyCommentList.get(holder.getAdapterPosition());
                                    historyComment1.state = HistoryComment.STATE_DELETED;
                                    notifyItemChanged(holder.getAdapterPosition());
                                    statisticsDBOpenHelper.updateHistoryComment(historyComment.rpid,HistoryComment.STATE_DELETED,historyComment.like,historyComment.replyCount,new Date());
                                    DialogUtil.dialogMessage(context,"检查结果","评论被删除！");
                                }

                                @Override
                                public void shadowBanned(int like,int rcount) {
                                    progressDialog.dismiss();
                                    HistoryComment historyComment1 = historyCommentList.get(holder.getAdapterPosition());
                                    //仅在评论被shadowBan时加统计，删除就算了，因为评论很有很多情况都是用户自己删的
                                    //如果检查前是正常，但是扫描后shadowBan，则是秋后算账
                                    if (historyComment.state.equals(HistoryComment.STATE_NORMAL) && !statisticsDBOpenHelper.checkBannedCommentIsExists(historyComment.rpid)) {
                                        statisticsDBOpenHelper.insertBannedComment(new BannedCommentBean(historyComment.commentArea, historyComment.rpid, historyComment.comment, BannedCommentBean.BANNED_TYPE_SHADOW_BAN_RECKONING, new Date(),  BannedCommentBean.CHECKED_NO_CHECK));
                                    }
                                    historyComment1.state = HistoryComment.STATE_SHADOW_BAN;
                                    historyComment1.like = like;
                                    historyComment1.replyCount = rcount;
                                    statisticsDBOpenHelper.updateHistoryComment(historyComment.rpid,HistoryComment.STATE_SHADOW_BAN,like,rcount,new Date());
                                    notifyItemChanged(holder.getAdapterPosition());
                                    DialogUtil.dialogMessage(context,"检查结果","评论处于shadowBan状态");
                                }

                                @Override
                                public void ok(int like,int rcount) {
                                    progressDialog.dismiss();
                                    //notifyDataSetChanged();
                                    int index = holder.getAdapterPosition();
                                    HistoryComment historyComment1 = historyCommentList.get(index);
                                    historyComment1.state = HistoryComment.STATE_NORMAL;
                                    historyComment1.like = like;
                                    historyComment1.replyCount = rcount;
                                    statisticsDBOpenHelper.updateHistoryComment(historyComment.rpid,HistoryComment.STATE_NORMAL,like,rcount,new Date());
                                    notifyItemChanged(index);
                                    new AlertDialog.Builder(context)
                                            .setTitle("检查结果")
                                            .setMessage("评论正常，是否继续翻遍评论区？")
                                            .setNegativeButton("取消",new VoidDialogInterfaceOnClickListener())
                                            .setPositiveButton("继续", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    searchThroughoutTheCommentArea(historyComment,index);
                                                }
                                            })
                                            .show();
                                    //DialogUtil.dialogMessage(context,"检查结果","评论正常，是否继续翻遍评论区？");
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
                                    statisticsDBOpenHelper.updateHistoryComment(historyComment.rpid,HistoryComment.STATE_NORMAL,like,replyCount,new Date());
                                    HistoryComment historyComment1 = historyCommentList.get(holder.getAdapterPosition());
                                    historyComment1.state = HistoryComment.STATE_NORMAL;
                                    historyComment1.like = like;
                                    historyComment1.replyCount = replyCount;
                                    notifyItemChanged(holder.getAdapterPosition());
                                    DialogUtil.dialogMessage(context,"检查结果","此回复评论正常显示！");
                                }

                                @Override
                                public void rootCommentIsShadowBan() {
                                    progressDialog.dismiss();
                                    HistoryComment historyComment1 = historyCommentList.get(holder.getAdapterPosition());
                                    historyComment1.state = HistoryComment.STATE_SHADOW_BAN;
                                    statisticsDBOpenHelper.updateHistoryComment(historyComment.rpid,HistoryComment.STATE_SHADOW_BAN,historyComment.like,historyComment.replyCount,new Date());
                                    notifyItemChanged(holder.getAdapterPosition());
                                    DialogUtil.dialogMessage(context,"检查结果","你的根评论后期遭到shadowBan，此条回复评论被连累了！");
                                }

                                @Override
                                public void invisible(int like, int replyCount) {
                                    progressDialog.dismiss();
                                    HistoryComment historyComment1 = historyCommentList.get(holder.getAdapterPosition());
                                    historyComment1.state = HistoryComment.STATE_INVISIBLE;
                                    historyComment1.like = like;
                                    historyComment1.replyCount = replyCount;
                                    statisticsDBOpenHelper.updateHistoryComment(historyComment.rpid,HistoryComment.STATE_INVISIBLE,like,replyCount,new Date());
                                    notifyItemChanged(holder.getAdapterPosition());
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
                        .setNeutralButton("删除记录", (dialog, which) -> new AlertDialog.Builder(context).setMessage("确认删除吗？")
                                .setNegativeButton("手滑了", new VoidDialogInterfaceOnClickListener())
                                .setPositiveButton("确认", (dialog14, which2) -> {
                                    if (statisticsDBOpenHelper.deleteHistoryComment(historyComment.rpid) != 0) {
                                        historyCommentList.remove(holder.getAdapterPosition());
                                        notifyItemRemoved(holder.getAdapterPosition());
                                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                                    }
                                }).show())
                        .show();
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            View view = View.inflate(context,R.layout.edit_text, null);
            EditText editText = view.findViewById(R.id.edit_text);
            editText.setText(historyComment.comment);
            new AlertDialog.Builder(context)
                    .setTitle("编辑重发")
                    .setView(view)
                    .setNegativeButton("取消",new VoidDialogInterfaceOnClickListener())
                    .setPositiveButton("发送", (dialog, which) -> {
                        ProgressBarDialog progressBarDialog = new ProgressBarDialog.Builder(context)
                                .setTitle("重发评论")
                                .setMessage("正在发送...")
                                .setIndeterminate(true)
                                .setCancelable(false)
                                .show();

                        commentPresenter.resendComment(historyComment, editText.getText().toString(), new CommentPresenter.ResendCommentCallBack() {
                            @Override
                            public void onSendFailed(int code, String msg) {
                                progressBarDialog.dismiss();
                                DialogUtil.dialogMessage(context, "发送失败", "message:"+msg+"\ncode:"+code);
                            }

                            @Override
                            public void onNewProgress(int progress, long sleepSeg,long waitTime) {
                                progressBarDialog.setMessage("评论已发送，等待("+progress*sleepSeg+"/"+waitTime+")ms后检查状态...");
                                progressBarDialog.setProgress(progress);
                            }

                            @Override
                            public void onSendSuccessAndSleep(long waitTime) {
                                progressBarDialog.setIndeterminate(false);
                                progressBarDialog.setMessage("评论已发送，等待(0/"+waitTime+")ms后检查状态...");
                            }

                            @Override
                            public void onResentComment(CommentAddResult commentAddResult) {
                                progressBarDialog.setIndeterminate(true);
                                dialogCommCheckWorker.checkComment(historyComment.commentArea,commentAddResult.rpid,commentAddResult.parent, commentAddResult.root, editText.getText().toString(),false,progressBarDialog);
                            }

                            @Override
                            public void onNetworkError(Throwable th) {
                                progressBarDialog.dismiss();
                                DialogUtil.dialogMessage(context, "网络错误", th.getMessage());
                            }
                        });
                    })
                    .show();
            return false;
        });
    }


    @Override
    public int getItemCount() {
        return historyCommentList.size();
    }

    public void addData(List<HistoryComment> historyCommentList) {
        Collections.reverse(historyCommentList);
        this.historyCommentList.addAll(0, historyCommentList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        View itemView;
        TextView txv_comment, txv_like, txv_reply_count,txv_info, txv_date;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            txv_comment = itemView.findViewById(R.id.txv_comment_content);
            txv_like = itemView.findViewById(R.id.txv_like);
            txv_info = itemView.findViewById(R.id.txv_info);
            txv_date = itemView.findViewById(R.id.txv_date);
            txv_reply_count = itemView.findViewById(R.id.txv_reply_count);
        }
    }

    private void searchThroughoutTheCommentArea(HistoryComment historyComment,int index){
        ProgressDialog progressDialog = DialogUtil.newProgressDialog(context,"寻找中","准备……");
        progressDialog.show();
        commentReviewPresenter.searchThroughoutTheCommentArea(historyComment.commentArea, historyComment.rpid, new CommentReviewPresenter.SearchTTCommAreaCallback() {
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
                statisticsDBOpenHelper.updateHistoryComment(historyComment.rpid,HistoryComment.STATE_SHADOW_BAN,historyComment.like,historyComment.replyCount,new Date());
                HistoryComment historyComment1 = historyCommentList.get(index);
                historyComment1.state = HistoryComment.STATE_SHADOW_BAN;
                historyComment1.like = historyComment.like;
                historyComment1.replyCount = historyComment.replyCount;
                notifyItemChanged(index);
                DialogUtil.dialogMessage(context,"寻找评论","无账号下翻遍了评论区，未找到你的评论！评论审核中或ShadowBan+");
            }

            @Override
            public void onNetworkError(Throwable th) {
                progressDialog.dismiss();
                DialogUtil.dialogMessage(context,"网络错误",th.getLocalizedMessage());
            }
        });
    }
}
