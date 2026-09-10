package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import icu.freedomIntrovert.async.EventHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.async.DeleteCommentTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.ReviewCommentStatusTask;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.picturestorage.PictureStorage;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.AccountSelectionDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.AppealCommentDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.CheckCommentAreaDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.ScanSensitiveWordDialog;

public class HistoryCommentAdapter extends RecyclerView.Adapter<HistoryCommentAdapter.ViewHolder> implements BiliBiliApiRequestHandler.DialogErrorHandle.OnDialogMessageListener {
    HistoryCommentActivity context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    List<HistoryComment> historyCommentList;
    CommentManipulator commentManipulator;
    DialogCommCheckWorker dialogCommCheckWorker;
    Config config;
    AccountManger accountManger;
    boolean 花里胡哨;

    public HistoryCommentAdapter(HistoryCommentActivity context, CommentManipulator commentManipulator,
                                 StatisticsDBOpenHelper statisticsDBOpenHelper) {
        this.context = context;
        config = Config.getInstance(context);
        this.statisticsDBOpenHelper = statisticsDBOpenHelper;
        Config config = Config.getInstance(context);
        this.commentManipulator = commentManipulator;
        花里胡哨 = config.get花里胡哨Enable();
        this.accountManger = AccountManger.getInstance(context);
        this.dialogCommCheckWorker = new DialogCommCheckWorker(context);
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
        holder.txv_like.setText(formatCount(historyComment.like));
        if (historyComment.like > 1000 || historyComment.replyCount > 1000) {
            holder.txv_like.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 7);
            holder.txv_reply_count.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 7);
        }
        holder.txv_reply_count.setText(formatCount(historyComment.replyCount));
        holder.itemView.setOnClickListener(v -> {
            showCommentInfoDialog(historyComment, holder);
        });
        if (historyComment.appealState == HistoryComment.APPEAL_STATE_NULL){
            holder.txv_info.setBackground(null);
        } else if (historyComment.appealState == HistoryComment.APPEAL_STATE_NO_COMMENT){
            holder.txv_info.setBackground(context.getDrawable(R.color.appeal_no_comment_green));
        } else if (historyComment.appealState == HistoryComment.APPEAL_STATE_SUCCESS){
            holder.txv_info.setBackground(context.getDrawable(R.color.appeal_success_red));
        }
        //重发功能已移除，因为使用率极低，替代方案：定位评论，到哔哩哔哩App发送
        /*holder.itemView.setOnLongClickListener(v -> {
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
        });*/
    }

    private void showCommentInfoDialog(HistoryComment historyComment, ViewHolder holder) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_history_comment_info, null, true);
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
        TextView txv_uid = dialogView.findViewById(R.id.txv_uid);
        TextView txv_appeal_state = dialogView.findViewById(R.id.txv_appeal_state);
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

        txv_uid.setText(String.valueOf(historyComment.uid));
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
            builder.setNegativeButton("更新状态", null);
        }

        AlertDialog infoDialog = builder
                .setNeutralButton("更多选项", null)
                .show();
        Button buttonMore = infoDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        buttonMore.setOnClickListener(v1 -> showSubMenu(buttonMore, infoDialog, holder, historyComment));
        Button buttonRecheck = infoDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        buttonRecheck.setOnClickListener(v -> {
            infoDialog.dismiss();
            if (historyComment.uid != 0) {
                recheckComment(historyComment, null, holder);
            } else {
                //如果没有UID，也就是之前没有记录UID，选择账号检查
                selectAccountToCheck(historyComment, holder);
            }
        });
        //提供一个可以自由选择的选项，以防账号不正确，但是不建议选择与评论发布者不一样的账号。
        buttonRecheck.setOnLongClickListener(v -> {
            infoDialog.dismiss();
            selectAccountToCheck(historyComment, holder);
            return true;
        });
        if (historyComment.appealState == HistoryComment.APPEAL_STATE_NULL){
            txv_appeal_state.setText("未进行申诉");
        } else if (historyComment.appealState == HistoryComment.APPEAL_STATE_SUCCESS){
            txv_appeal_state.setText("申诉提交成功");
        } else if (historyComment.appealState == HistoryComment.APPEAL_STATE_NO_COMMENT) {
            txv_appeal_state.setText("无可申诉评论");
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
            return true;
        });
        /*popupMenu.getMenu().add("删除评论与历史记录").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                return false;
            }
        });*/
        if (!historyComment.lastState.equals(HistoryComment.STATE_NORMAL) && !historyComment.lastState.equals(HistoryComment.STATE_SENSITIVE)) {
            popupMenu.getMenu().add("尝试申诉").setOnMenuItemClickListener(item -> {
                System.out.println("尝试申诉");
                AppealCommentDialog.show(context, historyComment, new AppealCommentDialog.ResultCallback(context) {
                    @Override
                    public void onNoCommentToAppeal(String successToast) {
                        super.onNoCommentToAppeal(successToast);
                        //statisticsDBOpenHelper.updateHistoryCommentLastState(historyComment.rpid, HistoryComment.STATE_SUSPECTED_NO_PROBLEM);
                        //notifyItemChanged(holder.getBindingAdapterPosition());
                    }

                    @Override
                    public void onComplete() {
                        context.reloadData(null);
                    }
                });
                return true;
            });
            popupMenu.getMenu().add("扫描敏感词").setOnMenuItemClickListener(item -> {
                dialog.dismiss();
                ScanSensitiveWordDialog.show(context, historyComment, comment -> {
                    //检查结果返回更新列表
                    historyCommentList.set(holder.getBindingAdapterPosition(),statisticsDBOpenHelper.getHistoryComment(comment.rpid));
                    notifyItemChanged(holder.getBindingAdapterPosition());
                });
                return true;
            });
            popupMenu.getMenu().add("检查评论区").setOnMenuItemClickListener(item -> {
                new CheckCommentAreaDialog(context).show(historyComment);
                return true;
            });
        }

        if (!historyComment.lastState.equals(HistoryComment.STATE_SENSITIVE)) {
            popupMenu.getMenu().add("删除B站上的评论").setOnMenuItemClickListener(item -> {
                new AlertDialog.Builder(context)
                        .setTitle("确认删除吗？")
                        .setMessage("这会删除你在B站上的评论，但不会删除你在本反诈上的历史记录")
                        .setNegativeButton("手滑了",null)
                        .setPositiveButton("确定", (dialog1, which) -> {
                            new DeleteCommentTask(context, historyComment, new DeleteCommentTask.EventHandler() {
                                @Override
                                public void onAccountNotFound(long uid) {
                                    Toast.makeText(context, "删除失败，未找到账号UID："+uid, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onSuccess() {
                                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Throwable th) {
                                    Toast.makeText(context, th.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }).execute();
                        })
                        .show();
                return true;
            });
            popupMenu.getMenu().add("定位评论").setOnMenuItemClickListener(item -> {
                CommentLocator.lunch(context, historyComment.commentArea.type,
                        historyComment.commentArea.oid, historyComment.rpid,
                        historyComment.root, historyComment.commentArea.sourceId);
                return true;
            });
            popupMenu.getMenu().add("监控评论").setOnMenuItemClickListener(item -> {
                CommentUtil.toMonitoringComment(context,historyComment);
                return true;
            });
        }
        // 显示子菜单
        popupMenu.show();
    }

    private void selectAccountToCheck(HistoryComment comment, ViewHolder holder) {
        AccountSelectionDialog.show(context, "选择账号（请与评论发送者一致）",null, account -> {
            recheckComment(comment, account, holder);
        });
    }

    private void recheckComment(HistoryComment comment, Account account, ViewHolder holder) {
        ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, null, "复查中……");
        progressDialog.setCancelable(false);
        progressDialog.show();
        new ReviewCommentStatusTask(context, new HistoryComment[]{comment}, account, new ReviewCommentStatusTask.EventHandler() {
            @Override
            public void onCookieFailed(Account account) {
                progressDialog.dismiss();
                dialogMessage("检查失败", String.format("用户：%s(%s)的cookie已失效", account.uname, account.uid));
            }

            @Override
            public void onNoAccount(long uid) {
                progressDialog.dismiss();
                dialogMessage("检查失败", "未找到用户，UID：" + uid);
            }

            @Override
            public void onAreaDead(HistoryComment historyComment, int index) {
                progressDialog.dismiss();
                dialogMessage("检查失败", "评论区已关闭，暂未更新评论状态");
            }

            @Override
            public void onRootCommentFailed(HistoryComment historyComment, int index) {
                progressDialog.dismiss();
                dialogMessage("检查失败", "根评论遭到删除或ShadowBan，暂未更新评论状态，根评论id:" + historyComment.root);
            }

            @Override
            public void onCheckResult(HistoryComment historyComment, int index) {
                progressDialog.dismiss();
                String resultMessage;
                switch (historyComment.lastState) {
                    case HistoryComment.STATE_SUSPECTED_NO_PROBLEM:
                        resultMessage =  "评论疑似审核中，但你之前申诉说没有可申诉的，请等待十分钟左右再来复查";
                        break;
                    case HistoryComment.STATE_UNDER_REVIEW:
                        resultMessage = "有账号:Y,无账号:Y,无账号seek_rpid:N，评论审核中或ShadowBan+";
                        break;
                    default:
                        resultMessage = HistoryComment.getStateDesc(historyComment.lastState);
                }
                notifyItemChanged(holder.getBindingAdapterPosition());
                dialogMessage("检查结果",resultMessage);
            }

            @Override
            public void onError(Throwable th) {
                progressDialog.dismiss();
                DialogUtil.dialogError(context,th);
            }
        }).execute();
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
    public void set花里胡哨Enable(boolean enable) {
        this.花里胡哨 = enable;
        notifyDataSetChanged();
    }

    public static String formatCount(int count) {
        if (count < 10000) {
            return String.valueOf(count);
            // } else if (count < 10000){
            //    return String.format(Locale.getDefault(),"%.1f千", count / 1000.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f万", count / 10000.0);
        }
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
            txv_banned_type = itemView.findViewById(R.id.txv_old_status);
            imgv_banned_type = itemView.findViewById(R.id.img_old_status);
            imgv_cover_image = itemView.findViewById(R.id.cover_image);
        }
    }

    @Override
    public void dialogMessage(String title, String message) {
        DialogUtil.dialogMessage(context, title, message);
    }
}
