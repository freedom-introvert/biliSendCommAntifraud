package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;

@SuppressLint("UseCompatLoadingForDrawables")
public class BatchCheckAdapter extends RecyclerView.Adapter<BatchCheckAdapter.ViewHolder> {
    private final Context context;

    private final List<String> oldStatusList = new ArrayList<>();
    private final List<HistoryComment> checkedComments = new ArrayList<>();

    private HistoryComment checkingComment;

    public BatchCheckAdapter(Context context) {
        this.context = context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_one_batch_checking_comment, parent, false));

    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == checkedComments.size()) {
            holder.txv_comment_content.setText(checkingComment.comment);
            setStatus(checkingComment.lastState,holder.txv_old_status,holder.img_old_status);
            holder.new_status_icon_frame.removeAllViews();
            holder.new_status_icon_frame.addView(new ProgressBar(context));
            return;
        }
        HistoryComment comment = checkedComments.get(position);
        holder.txv_comment_content.setText(comment.comment);
        setStatus(oldStatusList.get(position),holder.txv_old_status,holder.img_old_status);

        ImageView imageView = new ImageView(context);
        holder.new_status_icon_frame.removeAllViews();
        holder.new_status_icon_frame.addView(imageView);
        setStatus(comment.lastState,holder.txv_new_status,imageView);
    }

    @Override
    public int getItemCount() {
        int size = checkedComments.size();
        if (checkingComment != null) {
            size++;
        }
        return size;
    }

    private void setStatus(String status, TextView textView, ImageView imageView) {
        switch (status) {
            case HistoryComment.STATE_NORMAL:
                imageView.setImageDrawable(context.getDrawable(R.drawable.normal));
                textView.setText("该评论正常");
                break;
            case HistoryComment.STATE_SHADOW_BAN:
                imageView.setImageDrawable(context.getDrawable(R.drawable.hide));
                textView.setText("仅自己可见");
                break;
            case HistoryComment.STATE_UNDER_REVIEW:
                imageView.setImageDrawable(context.getDrawable(R.drawable.i));
                textView.setText("疑似审核中");
                break;
            case HistoryComment.STATE_DELETED:
                imageView.setImageDrawable(context.getDrawable(R.drawable.deleted));
                textView.setText("已被删除");
                break;
            case HistoryComment.STATE_SENSITIVE:
                imageView.setImageDrawable(context.getDrawable(R.drawable.sensitive));
                textView.setText("包含敏感词");
                break;
            case HistoryComment.STATE_INVISIBLE:
                imageView.setImageDrawable(context.getDrawable(R.drawable.ghost));
                textView.setText("评论被隐身");
                break;
            case HistoryComment.STATE_UNKNOWN:
                imageView.setImageDrawable(context.getDrawable(R.drawable.unknown));
                textView.setText("未知状态");
                break;
            case HistoryComment.STATE_SUSPECTED_NO_PROBLEM:
                imageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_access_time_24));
                textView.setText("疑似正常");
                break;
            default:
                textView.setText(status);
        }
    }

    public void setCheckingComment(HistoryComment historyComment) {
        checkingComment = historyComment;
        oldStatusList.add(historyComment.lastState);
        notifyItemInserted(getItemCount() - 1);
    }


    public void overCheckComment(String newStatus) {
        if (checkingComment == null) {
            throw new IllegalStateException("overCheckComment() called while no checking comment");
        }
        checkingComment.lastState = newStatus;
        checkedComments.add(checkingComment);
        checkingComment = null;
        notifyItemChanged(getItemCount() - 1);

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img_old_status = itemView.findViewById(R.id.img_old_status);
        TextView txv_old_status = itemView.findViewById(R.id.txv_old_status);
        FrameLayout new_status_icon_frame = itemView.findViewById(R.id.new_status_icon_frame);
        TextView txv_new_status = itemView.findViewById(R.id.txv_new_status);
        TextView txv_comment_content = itemView.findViewById(R.id.txv_comment_content);

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
