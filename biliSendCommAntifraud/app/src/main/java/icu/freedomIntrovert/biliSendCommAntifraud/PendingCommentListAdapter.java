package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;

public class PendingCommentListAdapter extends RecyclerView.Adapter<PendingCommentListAdapter.ViewHolder> {

    private final Context context;
    private final StatisticsDBOpenHelper helper;
    private final List<Comment> comments;
    private final DialogCommCheckWorker worker;

    public PendingCommentListAdapter(Context context) {
        this.context = context;
        helper = new StatisticsDBOpenHelper(context);
        comments = helper.getAllPendingCheckComments();
        Config config = new Config(context);
        worker = new DialogCommCheckWorker(context, config, helper, new CommentManipulator(config.getCookie(), config.getDeputyCookie()), new CommentUtil(context));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pending_check_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.txv_comment.setText(comment.comment);
        holder.txv_info.setText(comment.commentArea.sourceId);
        holder.txv_date.setText(comment.getFormatDateFor_yMdHms());
        holder.itemView.setOnClickListener(view -> {
            new AlertDialog.Builder(context)
                    .setTitle("确认检查此评论")
                    .setMessage(comment.comment)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        ProgressBarDialog progressBarDialog = new ProgressBarDialog.Builder(context)
                                .setTitle("检查中")
                                .setMessage("准备检查评论……")
                                .setIndeterminate(true)
                                .setCancelable(false)
                                .show();
                        worker.setExitListener(new OnExitListener() {
                            @Override
                            public void onNewCommentRpid(long rpid) {
                                comments.remove(holder.getBindingAdapterPosition());
                                notifyItemRemoved(holder.getBindingAdapterPosition());
                            }
                        });
                        worker.checkComment(comment, progressBarDialog);
                    })
                    .setNegativeButton(android.R.string.cancel, new VoidDialogInterfaceOnClickListener())
                    .setNeutralButton("删除", (dialogInterface, i) -> {
                        helper.deletePendingCheckComment(comment.rpid);
                        comments.remove(holder.getBindingAdapterPosition());
                        notifyItemRemoved(holder.getBindingAdapterPosition());
                    })
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txv_comment, txv_info, txv_date;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txv_comment = itemView.findViewById(R.id.txv_comment_content);
            txv_info = itemView.findViewById(R.id.txv_info);
            txv_date = itemView.findViewById(R.id.txv_date);
        }
    }
}
