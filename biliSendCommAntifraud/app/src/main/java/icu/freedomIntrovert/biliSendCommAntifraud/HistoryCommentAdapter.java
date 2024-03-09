package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import icu.freedomIntrovert.async.EventMessage;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.ResendCommentTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.ReviewCommentStatusTask;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.AppealDialogPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.picturestorage.PictureStorage;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;

public class HistoryCommentAdapter extends RecyclerView.Adapter<HistoryCommentAdapter.ViewHolder> implements BiliBiliApiRequestHandler.DialogErrorHandle.OnDialogMessageListener {
    HistoryCommentActivity context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    List<HistoryComment> historyCommentList;
    CommentManipulator commentManipulator;
    DialogCommCheckWorker dialogCommCheckWorker;
    Config config;
    boolean 花里胡哨;

    public HistoryCommentAdapter(HistoryCommentActivity context, StatisticsDBOpenHelper statisticsDBOpenHelper) {
        this.context = context;
        config = new Config(context);
        this.statisticsDBOpenHelper = statisticsDBOpenHelper;
        Config config = new Config(context);
        commentManipulator = new CommentManipulator(config.getCookie(), config.getDeputyCookie());
        this.dialogCommCheckWorker = new DialogCommCheckWorker(context, config, statisticsDBOpenHelper, commentManipulator, new CommentUtil(context));
        花里胡哨 = config.get花里胡哨Enable();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_history_comment, parent, false);
        return new ViewHolder(itemView);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryComment historyComment = historyCommentList.get(position);
        holder.txv_comment.setText(historyComment.comment);
        if (historyComment.hasPictures()) {
            holder.imgv_cover_image.setImageDrawable(context.getDrawable(R.drawable.comment_long_pic));
        } else if (historyComment.root != 0) {
            holder.imgv_cover_image.setImageDrawable(context.getDrawable(R.drawable.comment_long_reply));
        } else {
            holder.imgv_cover_image.setImageDrawable(context.getDrawable(R.drawable.comment_long));
        }
        /*
        评论区状态颜色设置
        默认：没有检查过
        蓝色：评论区没有被戒严
        黄色：评论仅在此评论区被ban（或者此评论区特有的敏感词）
        绿色：评论在任何地方都被屏蔽，评论区是正常的
        红色：评论区被戒严（同时也统计在戒严评论区列表）
         */
        switch (historyComment.checkedArea) {
            case HistoryComment.CHECKED_NO_CHECK:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.GRAY));
                break;
            case HistoryComment.CHECKED_NOT_MARTIAL_LAW:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.blue));
                break;
            case HistoryComment.CHECKED_ONLY_BANNED_IN_THIS_AREA:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.yellow));
                break;
            case HistoryComment.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.green));
                break;
            case HistoryComment.CHECKED_MARTIAL_LAW:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.red));
                break;
        }
        switch (historyComment.lastState) {
            case HistoryComment.STATE_NORMAL:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.normal));
                holder.txv_banned_type.setText("该评论正常");
                break;
            case HistoryComment.STATE_SHADOW_BAN:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.hide));
                if (historyComment.firstState != null && historyComment.firstState.equals(HistoryComment.STATE_NORMAL)) {
                    holder.txv_banned_type.setText("仅自己可见(秋后算账)");
                } else {
                    holder.txv_banned_type.setText("仅自己可见");
                }
                break;
            case HistoryComment.STATE_UNDER_REVIEW:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.i));
                holder.txv_banned_type.setText("疑似审核中");
                break;
            case HistoryComment.STATE_DELETED:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.deleted));
                holder.txv_banned_type.setText("已被删除");
                break;
            case HistoryComment.STATE_SENSITIVE:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.sensitive));
                holder.txv_banned_type.setText("包含敏感词");
                break;
            case HistoryComment.STATE_INVISIBLE:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.ghost));
                holder.txv_banned_type.setText("评论被隐身");
                break;
            case HistoryComment.STATE_UNKNOWN:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.unknown));
                holder.txv_banned_type.setText("未知状态");
                break;
            case HistoryComment.STATE_SUSPECTED_NO_PROBLEM:
                holder.imgv_banned_type.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_access_time_24));
                holder.txv_banned_type.setText("疑似正常");
                break;
        }

        if (花里胡哨) {
            switch (historyComment.lastState) {
                case HistoryComment.STATE_DELETED:
                    holder.txv_comment.setTextColor(context.getResources().getColor(R.color.red));
                    holder.txv_comment.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
                    break;
                case HistoryComment.STATE_SHADOW_BAN:
                    holder.txv_comment.setTextColor(context.getResources().getColor(R.color.red));
                    holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
                    break;
                case HistoryComment.STATE_INVISIBLE:
                    holder.txv_comment.setTextColor(context.getResources().getColor(R.color.gray));
                    holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
                    break;
                case HistoryComment.STATE_UNDER_REVIEW:
                    holder.txv_comment.setTextColor(context.getResources().getColor(R.color.orange));
                    holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
                    break;
                case HistoryComment.STATE_SUSPECTED_NO_PROBLEM:
                    holder.txv_comment.setTextColor(context.getResources().getColor(R.color.blue));
                    holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
                    break;
                case HistoryComment.STATE_SENSITIVE:
                    holder.txv_comment.setTextColor(context.getResources().getColor(R.color.dark_violet));
                    holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
                    break;
                default:
                    holder.txv_comment.setTextColor(context.getResources().getColor(R.color.dark_font));
                    holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
                    break;
            }
        } else {
            holder.txv_comment.setTextColor(context.getResources().getColor(R.color.dark_font));
            holder.txv_comment.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG);
        }
        holder.txv_date.setText(historyComment.getFormatDateFor_yMd());
        holder.txv_info.setText(historyComment.commentArea.sourceId);
        holder.txv_like.setText(String.valueOf(historyComment.like));
        holder.txv_reply_count.setText(String.valueOf(historyComment.replyCount));
        holder.itemView.setOnClickListener(v -> {
            showCommentInfoDialog(historyComment,holder);
        });
        holder.itemView.setOnLongClickListener(v -> {
            View view = View.inflate(context, R.layout.edit_text, null);
            EditText editText = view.findViewById(R.id.edit_text);
            editText.setText(historyComment.comment);
            new AlertDialog.Builder(context)
                    .setTitle("编辑重发")
                    .setView(view)
                    .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                    .setPositiveButton("发送", (dialog, which) -> {
                        ProgressBarDialog progressBarDialog = new ProgressBarDialog.Builder(context)
                                .setTitle("重发评论")
                                .setMessage("正在发送...")
                                .setIndeterminate(true)
                                .setCancelable(false)
                                .show();
                        ResendCommentHandler handler = new ResendCommentHandler(this, progressBarDialog, historyComment, holder);
                        new ResendCommentTask(handler, commentManipulator, config, editText.getText().toString(), historyComment).execute();
                    })
                    .show();
            return false;
        });
    }

    private void showCommentInfoDialog(HistoryComment historyComment,ViewHolder holder){
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_history_comment_info, null, true);//View.inflate(context, R.layout.dialog_history_comment_info, null);
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
        TextView txv_first_state = dialogView.findViewById(R.id.txv_first_state);
        TextView txv_checked_area = dialogView.findViewById(R.id.txv_checked_area);
        SensitiveScanResult scr = historyComment.sensitiveScanResult;
        if (scr != null) {
            ForegroundColorSpan greenSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.green));
            ForegroundColorSpan redSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.red));

            SpannableStringBuilder builder0 = new SpannableStringBuilder(historyComment.comment);
            builder0.setSpan(greenSpan, 0, scr.normalPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            builder0.setSpan(redSpan, scr.normalPosition, scr.unusualPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            txv_comment.setText(builder0);
            System.out.println(scr.normalPosition);
        } else {
            txv_comment.setText(historyComment.comment);
        }


        txv_last_state.setText(HistoryComment.getStateDesc(historyComment.lastState));
        txv_first_state.setText(HistoryComment.getStateDesc(historyComment.firstState));
        txv_source_id.setText(historyComment.commentArea.sourceId);
        txv_oid.setText(String.valueOf(historyComment.commentArea.oid));
        txv_area_type.setText(historyComment.commentArea.getAreaTypeDesc());
        txv_rpid.setText(String.valueOf(historyComment.rpid));
        txv_parent.setText(String.valueOf(historyComment.parent));
        txv_root.setText(String.valueOf(historyComment.root));
        txv_check_date.setText(historyComment.getFormatLastCheckDateFor_yMdHms());
        txv_send_date.setText(historyComment.getFormatDateFor_yMdHms());
        switch (historyComment.checkedArea) {
            case HistoryComment.CHECKED_NO_CHECK:
                txv_checked_area.setText("未检查");
                break;
            case HistoryComment.CHECKED_NOT_MARTIAL_LAW:
                txv_checked_area.setText("只检查过未戒严");
                break;
            case HistoryComment.CHECKED_ONLY_BANNED_IN_THIS_AREA:
                txv_checked_area.setText("仅在在此评论区被ban");
                break;
            case HistoryComment.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA:
                txv_checked_area.setText("评论区一切正常，该评论在任何评论区都被ban");
                break;
            case HistoryComment.CHECKED_MARTIAL_LAW:
                txv_checked_area.setText("评论区被戒严");
        }
        List<Comment.PictureInfo> pictureInfoList = historyComment.getPictureInfoList();
        if (pictureInfoList != null) {
            RecyclerView rv_images = dialogView.findViewById(R.id.rv_pictures);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
            linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            rv_images.setLayoutManager(linearLayoutManager);
            PicturesAdapter picturesAdapter = new PicturesAdapter(context, pictureInfoList, linearLayoutManager);
            rv_images.setAdapter(picturesAdapter);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener());
        //特殊的敏感评论，仅记录并未发送成功，无真正的rpid，所以无更新状态
        if (!HistoryComment.STATE_SENSITIVE.equals(historyComment.firstState)) {
            builder.setNegativeButton("更新状态", (dialog, which) -> {
                ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, null, "复查中……");
                progressDialog.setCancelable(false);
                progressDialog.show();
                ReviewCommentStatusHandler handle = new ReviewCommentStatusHandler(this, historyComment, progressDialog, holder);
                new ReviewCommentStatusTask(handle, commentManipulator, statisticsDBOpenHelper, historyComment).execute();
            });
        }
        AlertDialog infoDialog = builder
                .setNeutralButton("更多选项", null)
                .show();
        Button buttonMore = infoDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        buttonMore.setOnClickListener(v1 -> showSubMenu(buttonMore, infoDialog, holder, historyComment));
    }

    private static class ReviewCommentStatusHandler extends ReviewCommentStatusTask.EventHandler {
        private final HistoryCommentAdapter adapter;
        private final HistoryComment historyComment;
        private final ProgressDialog progressDialog;
        private final ViewHolder holder;

        public ReviewCommentStatusHandler(HistoryCommentAdapter adapter, HistoryComment historyComment, ProgressDialog progressDialog, ViewHolder holder) {
            super(new DialogErrorHandle(progressDialog, adapter));
            this.adapter = adapter;
            this.historyComment = historyComment;
            this.progressDialog = progressDialog;
            this.holder = holder;
        }

        @Override
        protected void handleEvent(EventMessage message) {
            switch (message.getWhat()) {
                case WHAT_ON_PAGE_TURN_FOR_NO_ACC_REPLY:
                    progressDialog.setMessage("正在无账号条件下查找评论回复列表，第" +
                            message.getObject(0, Integer.class) + "页");
                    break;
                case WHAT_ON_PAGE_TURN_FOR_HAS_ACC_REPLY:
                    progressDialog.setMessage("正在有账号条件下查找评论回复列表，第" +
                            message.getObject(0, Integer.class) + "页");
                    break;
                case WHAT_OK:
                    progressDialog.dismiss();
                    updateCommentInfo(message.getObject(0, BiliComment.class),
                            HistoryComment.STATE_NORMAL);
                    dialogCheckResult("该评论正常");
                    break;
                case WHAT_SHADOW_BANNED:
                    progressDialog.dismiss();
                    updateCommentInfo(message.getObject(0, BiliComment.class),
                            HistoryComment.STATE_SHADOW_BAN);
                    dialogCheckResult("评论处于shadowBan状态");
                    break;
                case WHAT_DELETED:
                    progressDialog.dismiss();
                    historyComment.lastCheckDate = new Date();
                    historyComment.lastState = HistoryComment.STATE_DELETED;
                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                    dialogCheckResult("评论被删除！");
                    break;
                case WHAT_INVISIBLE:
                    progressDialog.dismiss();
                    updateCommentInfo(message.getObject(0, BiliComment.class),
                            HistoryComment.STATE_INVISIBLE);
                    dialogCheckResult("评论invisible，前端不可见！");
                    break;
                case WHAT_UNDER_REVIEW:
                    progressDialog.dismiss();
                    updateCommentInfo(message.getObject(0, BiliComment.class),
                            HistoryComment.STATE_UNDER_REVIEW);
                    dialogCheckResult("有账号:Y,无账号:Y,无账号seek_rpid:N，评论审核中或ShadowBan+");
                    break;
                case WHAT_REPLY_OK:
                    progressDialog.dismiss();
                    updateCommentInfo(message.getObject(0, BiliComment.class),
                            HistoryComment.STATE_NORMAL);
                    dialogCheckResult("此回复评论正常显示！");
                    break;
                case WHAT_ROOT_COMMENT_IS_SHADOW_BAN:
                    progressDialog.dismiss();
                    historyComment.lastState = HistoryComment.STATE_SHADOW_BAN;
                    historyComment.lastCheckDate = new Date();
                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                    dialogCheckResult("你的根评论后期遭到shadowBan，此条回复评论被连累了！");
                    break;
            }
        }

        private void updateCommentInfo(BiliComment resultComment, String newState) {
            historyComment.lastState = newState;
            historyComment.like = resultComment.like;
            historyComment.replyCount = resultComment.rcount;
            historyComment.lastCheckDate = new Date();
            adapter.notifyItemChanged(holder.getBindingAdapterPosition());
        }

        private void dialogCheckResult(String result) {
            DialogUtil.dialogMessage(adapter.context, "检查结果", result);
        }
    }

    private static class ResendCommentHandler extends ResendCommentTask.EventHandler {
        ProgressBarDialog progressBarDialog;
        HistoryComment historyComment;
        HistoryCommentAdapter adapter;
        DialogCommCheckWorker worker;
        ViewHolder holder;
        StatisticsDBOpenHelper helper;

        public ResendCommentHandler(HistoryCommentAdapter adapter,
                                    ProgressBarDialog progressBarDialog,
                                    HistoryComment historyComment,
                                    ViewHolder viewHolder) {
            super(new DialogErrorHandle(progressBarDialog, adapter));
            this.progressBarDialog = progressBarDialog;
            this.historyComment = historyComment;
            this.adapter = adapter;
            this.worker = adapter.dialogCommCheckWorker;
            this.holder = viewHolder;
            this.helper = adapter.statisticsDBOpenHelper;
        }

        @Override
        protected void handleEvent(EventMessage message) {
            switch (message.getWhat()) {
                case WHAT_ON_SEND_SUCCESS_AND_SLEEP:
                    progressBarDialog.setIndeterminate(false);
                    progressBarDialog.setMessage("评论已发送，等待(0/" + message.getObject(0, Long.class) + ")ms后检查状态...");
                    break;
                case WHAT_ON_NEW_PROGRESS:
                    int progress = message.getObject(0, Integer.class);
                    progressBarDialog.setMessage("评论已发送，等待(" +
                            progress * message.getObject(1, Long.class) +
                            "/" + message.getObject(2, Long.class) + ")ms后检查状态...");
                    progressBarDialog.setProgress(progress);
                    break;
                case WHAT_ON_RESENT_COMMENT:
                    progressBarDialog.setIndeterminate(true);
                    BiliComment c = message.getObject(0, CommentAddResult.class).reply;
                    worker.setExitListener(new OnExitListener() {
                        @Override
                        public void onNewCommentRpid(long rpid) {
                            HistoryComment comment = helper.getHistoryComment(c.rpid);
                            adapter.historyCommentList.add(0, comment);
                            adapter.notifyItemInserted(0);
                        }
                    });
                    worker.checkComment(new Comment(historyComment.commentArea,
                            c.rpid, c.parent, c.root, c.content.message,
                            null, new Date(c.ctime * 1000)), progressBarDialog);
                    break;
            }
        }
    }

    private void showSubMenu(Button button, DialogInterface dialog, ViewHolder holder, HistoryComment historyComment) {
        PopupMenu popupMenu = new PopupMenu(context, button);

        // 在子菜单中添加选项
        popupMenu.getMenu().add("删除记录").setOnMenuItemClickListener(item -> {
            dialog.dismiss();
            new AlertDialog.Builder(context).setMessage("确认删除吗？")
                    .setNegativeButton("手滑了", new VoidDialogInterfaceOnClickListener())
                    .setPositiveButton("确认", (dialog14, which2) -> {
                        if (statisticsDBOpenHelper.deleteHistoryComment(historyComment.rpid) != 0) {
                            List<Comment.PictureInfo> pictureInfoList = historyComment.getPictureInfoList();
                            if (pictureInfoList != null) {
                                for (Comment.PictureInfo pictureInfo : pictureInfoList) {
                                    PictureStorage.delete(context, pictureInfo.img_src);
                                }
                            }
                            historyCommentList.remove(holder.getBindingAdapterPosition());
                            notifyItemRemoved(holder.getBindingAdapterPosition());
                            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
            return false;
        });
        if (!historyComment.lastState.equals(HistoryComment.STATE_NORMAL) && !historyComment.lastState.equals(HistoryComment.STATE_SENSITIVE)) {
            popupMenu.getMenu().add("尝试申诉").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(@NonNull MenuItem item) {
                    AlertDialog dialog1 = new AlertDialog.Builder(context)
                            .setTitle("警告")
                            .setMessage("申诉评论不能指定要申诉的评论ID，只能指定评论区链接。审核[机/人]会阅读你在此评论区所有被ban评论，将没问题恢复，即使你删除了被ban评论！\n" +
                                    "测试评论区戒严会发送一个再删除、以及你删除重发，删除掉的就可能会被恢复，请留意将他们删除！")
                            .setNegativeButton("还是算了", new VoidDialogInterfaceOnClickListener())
                            .setNeutralButton("官方申诉网址", (dialog23, which1) -> {
                                Uri uri = Uri.parse("https://www.bilibili.com/h5/comment/appeal");
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                context.startActivity(intent);
                            })
                            .setPositiveButton("去申诉", (dialog24, which1) -> {
                                AppealDialogPresenter appealDialogPresenter = new AppealDialogPresenter(context, new Handler(), commentManipulator);
                                appealDialogPresenter.appeal(CommentUtil.sourceIdToUrl(historyComment.commentArea), historyComment.comment, new AppealDialogPresenter.CallBack() {

                                    @Override
                                    public void onRespInUI(int code, String toastText) {
                                        //如果这个时候还出现“无可申述评论”那么可能把评论状态误判了或者在某种审核中
                                        if (code == 12082) {
                                            statisticsDBOpenHelper.updateHistoryCommentLastState(historyComment.rpid, HistoryComment.STATE_SUSPECTED_NO_PROBLEM);
                                            DialogUtil.dialogMessage(context, "申诉结果", toastText + "\n可能因为检查评论时误判了或评论在某种处理或审核状态，等待一段时间后应该可以显示");
                                        } else {
                                            DialogUtil.dialogMessage(context, "申诉结果", toastText);
                                        }
                                    }

                                    @Override
                                    public void onNetErrInUI(String msg) {
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                                    }
                                });
                            })
                            .show();
                    return false;
                }
            });
        }
        if (!historyComment.lastState.equals(HistoryComment.STATE_NORMAL) && !historyComment.lastState.equals(HistoryComment.STATE_SENSITIVE)) {
            popupMenu.getMenu().add("扫描敏感词").setOnMenuItemClickListener(item -> {
                new AlertDialog.Builder(context)
                        .setMessage("确认扫描敏感词吗？")
                        .setNegativeButton(android.R.string.cancel, new VoidDialogInterfaceOnClickListener())
                        .setPositiveButton(android.R.string.ok, (dialog12, which) -> {
                            dialogCommCheckWorker.setExitListener(new OnExitListener() {
                                @Override
                                public void onCommentStatusUpdated(long rpid) {
                                    historyCommentList.set(holder.getBindingAdapterPosition(),statisticsDBOpenHelper.getHistoryComment(rpid));
                                    notifyItemChanged(holder.getBindingAdapterPosition());
                                }
                            });
                            dialog.dismiss();
                            dialogCommCheckWorker.scanSensitiveWord(historyComment);
                        }).show();
                return false;
            });
        }
        if (!historyComment.lastState.equals(HistoryComment.STATE_SENSITIVE)) {
            popupMenu.getMenu().add("定位评论").setOnMenuItemClickListener(item -> {
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                int areaType = historyComment.commentArea.type;
                Bundle extras = new Bundle();
                if (areaType == CommentArea.AREA_TYPE_VIDEO) {
                    intent.setClassName("tv.danmaku.bili", "tv.danmaku.bili.MainActivityV2");
                    intent.putExtra("TransferActivity", "com.bilibili.video.videodetail.VideoDetailsActivity");
                    extras.putString("id", String.valueOf(historyComment.commentArea.oid));

                    //根评论与评论回复的不同处理方法
                    if (historyComment.root != 0) {
                        extras.putString("comment_root_id", String.valueOf(historyComment.root));
                        extras.putString("comment_secondary_id", String.valueOf(historyComment.rpid));
                    } else {
                        extras.putString("comment_root_id", String.valueOf(historyComment.rpid));
                    }
                    extras.putString("comment_from_spmid", "im.notify-reply.0.0");
                    extras.putString("tab_index", "1");
                    intent.putExtra("transferUri", "bilibili://video/" + historyComment.commentArea.oid);
                } else if (areaType == CommentArea.AREA_TYPE_DYNAMIC11 || areaType == CommentArea.AREA_TYPE_DYNAMIC17) {
                    intent.setClassName("tv.danmaku.bili", "tv.danmaku.bili.MainActivityV2");
                    intent.putExtra("TransferActivity", "com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity");
                    if (historyComment.root != 0) {
                        extras.putString("commentId", String.valueOf(historyComment.root));
                    } else {
                        extras.putString("commentId", String.valueOf(historyComment.rpid));
                    }
                    extras.putString("anchor", String.valueOf(historyComment.rpid));
                    extras.putString("oid", String.valueOf(historyComment.commentArea.oid));
                    extras.putString("type", String.valueOf(areaType));
                    extras.putString("enterUri", "bilibili://following/detail/" + historyComment.commentArea.sourceId);
                    extras.putString("comment_from_spmid", "im.notify-reply.0.0");
                    extras.putString("enterName", "查看动态详情");
                    extras.putString("showEnter", "1");
                } else if (areaType == CommentArea.AREA_TYPE_ARTICLE) {
                    intent.setClassName("tv.danmaku.bili", "tv.danmaku.bili.MainActivityV2");
                    intent.putExtra("TransferActivity", "com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity");
                    if (historyComment.root != 0) {
                        extras.putString("commentId", String.valueOf(historyComment.root));
                    } else {
                        extras.putString("commentId", String.valueOf(historyComment.rpid));
                    }
                    extras.putString("anchor", String.valueOf(historyComment.rpid));
                    extras.putString("oid", String.valueOf(historyComment.commentArea.oid));
                    extras.putString("type", String.valueOf(areaType));
                    extras.putString("enterUri", "bilibili://article/" + historyComment.commentArea.oid);
                    extras.putString("comment_from_spmid", "im.notify-reply.0.0");
                    extras.putString("enterName", "查看文章详情");
                    extras.putString("showEnter", "1");
                }
                intent.putExtra("TransferExtras", extras);
                context.startActivity(intent);
                return false;
            });
        }
        // 显示子菜单
        popupMenu.show();
    }


    @Override
    public int getItemCount() {
        return historyCommentList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addSomeData(List<HistoryComment> historyCommentList) {
        Collections.reverse(historyCommentList);
        this.historyCommentList.addAll(0, historyCommentList);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void reloadData(List<HistoryComment> historyCommentList) {
        this.historyCommentList = historyCommentList;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void set花里胡哨Enable(boolean enable){
        this.花里胡哨 = enable;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        TextView txv_comment, txv_like, txv_reply_count, txv_info, txv_date, txv_banned_type;
        ImageView imgv_banned_type, imgv_cover_image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            txv_comment = itemView.findViewById(R.id.txv_comment_content);
            txv_like = itemView.findViewById(R.id.txv_like);
            txv_info = itemView.findViewById(R.id.txv_info);
            txv_date = itemView.findViewById(R.id.txv_date);
            txv_reply_count = itemView.findViewById(R.id.txv_reply_count);
            txv_banned_type = itemView.findViewById(R.id.txv_band_type);
            imgv_banned_type = itemView.findViewById(R.id.img_band_type);
            imgv_cover_image = itemView.findViewById(R.id.cover_image);
        }
    }

    @Override
    public void dialogMessage(String title, String message) {
        DialogUtil.dialogMessage(context, title, message);
    }
}
