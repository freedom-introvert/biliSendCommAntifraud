package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
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

import icu.freedomIntrovert.biliSendCommAntifraud.comment.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BandCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class BandCommentListAdapter extends RecyclerView.Adapter<BandCommentListAdapter.ViewHolder> {

    ArrayList<BandCommentBean> bandCommentBeanArrayList;
    Context context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;

    public BandCommentListAdapter(ArrayList<BandCommentBean> bandCommentBeanArrayList, Context context) {
        this.bandCommentBeanArrayList = bandCommentBeanArrayList;
        Collections.reverse(this.bandCommentBeanArrayList);
        this.context = context;
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.item_band_comment, null);
        return new ViewHolder(view);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BandCommentBean bandCommentBean = bandCommentBeanArrayList.get(position);

        holder.txv_info.setText(bandCommentBean.commentArea.sourceId);
        switch (bandCommentBean.checkedArea) {
            case BandCommentBean.CHECKED_NO_CHECK:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.GRAY));
                break;
            case BandCommentBean.CHECKED_NOT_MARTIAL_LAW:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.blue));
                break;
            case BandCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.red));
                break;
            case BandCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA:
                holder.txv_info.setTextColor(context.getResources().getColor(R.color.green));
                break;
        }
        holder.txv_comment.setText(bandCommentBean.comment);
        Log.d("yellow", holder.txv_info.getText().toString() + " " + holder.txv_info.getCurrentTextColor() + "" + holder);
        switch (bandCommentBean.bannedType) {
            case BandCommentBean.BANNED_TYPE_SHADOW_BAN:
                holder.imgv_band_type.setImageDrawable(context.getDrawable(R.drawable.hide));
                holder.txv_band_type.setText("仅自己可见");
                break;
            case BandCommentBean.BANNED_TYPE_QUICK_DELETE:
                holder.imgv_band_type.setImageDrawable(context.getDrawable(R.drawable.deleted));
                holder.txv_band_type.setText("被系统秒删");
                break;
            case BandCommentBean.BANNED_TYPE_SENSITIVE:
                holder.imgv_band_type.setImageDrawable(context.getDrawable(R.drawable.sensitive));
                holder.txv_band_type.setText("包含敏感词");
                break;
            case BandCommentBean.BANNED_TYPE_UNKNOWN:
                holder.imgv_band_type.setImageDrawable(context.getDrawable(R.drawable.unknown));
                holder.txv_band_type.setText("未知");
                break;
        }
        holder.txv_date.setText(bandCommentBean.getFormatDateFor_yMd());
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });

        holder.itemView.setOnClickListener(v -> {
            View dialogView = View.inflate(context, R.layout.dialog_band_comment_info, null);
            TextView txv_comment_content = dialogView.findViewById(R.id.txv_comment_content);
            TextView txv_oid = dialogView.findViewById(R.id.txv_oid);
            TextView txv_source_id = dialogView.findViewById(R.id.txv_source_id);
            TextView txv_band_type = dialogView.findViewById(R.id.txv_band_type);
            TextView txv_area_type = dialogView.findViewById(R.id.txv_area_type);
            TextView txv_send_date = dialogView.findViewById(R.id.txv_send_date);
            TextView txv_checkedArea = dialogView.findViewById(R.id.txv_checked_area);
            txv_comment_content.setText(bandCommentBean.comment);
            txv_oid.setText(bandCommentBean.commentArea.oid);
            txv_source_id.setText(bandCommentBean.commentArea.sourceId);
            switch (bandCommentBean.bannedType) {
                case BandCommentBean.BANNED_TYPE_SHADOW_BAN:
                    txv_band_type.setText("仅自己可见");
                    break;
                case BandCommentBean.BANNED_TYPE_QUICK_DELETE:
                    txv_band_type.setText("被系统秒删");
                    break;
                case BandCommentBean.BANNED_TYPE_SENSITIVE:
                    txv_band_type.setText("包含敏感词");
                    break;
                case BandCommentBean.BANNED_TYPE_UNKNOWN:
                    txv_band_type.setText("未知（直接去申诉等无法得知具体状态）");
            }
            switch (bandCommentBean.commentArea.areaType) {
                case CommentArea.AREA_TYPE_VIDEO:
                    txv_area_type.setText("视频");
                    break;
                case CommentArea.AREA_TYPE_ARTICLE:
                    txv_area_type.setText("专栏");
                    break;
                case CommentArea.AREA_TYPE_DYNAMIC11:
                case CommentArea.AREA_TYPE_DYNAMIC17:
                    txv_area_type.setText("动态");
                    break;
            }
            switch (bandCommentBean.checkedArea) {
                case BandCommentBean.CHECKED_NO_CHECK:
                    txv_checkedArea.setText("未检查");
                    break;
                case BandCommentBean.CHECKED_NOT_MARTIAL_LAW:
                    txv_checkedArea.setText("只检查过未戒严");
                    break;
                case BandCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA:
                    txv_checkedArea.setText("仅在在此评论区被ban");
                    break;
                case BandCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA:
                    txv_checkedArea.setText("评论区一切正常，该评论在任何评论区都被ban");
                    break;
            }


            txv_send_date.setText(bandCommentBean.getFormatDateFor_yMdHms());

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("评论详情")
                    .setView(dialogView)
                    .setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener())
                    .setNegativeButton("复制", (dialog13, which) -> {
                        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData mClipData = ClipData.newPlainText("Label", bandCommentBean.comment);
                        cm.setPrimaryClip(mClipData);
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("删除", (dialog12, which) -> {
                        new AlertDialog.Builder(context).setMessage("确认删除吗？")
                                .setNegativeButton("手滑了", new VoidDialogInterfaceOnClickListener())
                                .setPositiveButton("确认", (dialog14, which2) -> {
                                    if (statisticsDBOpenHelper.deleteBandComment(bandCommentBean.rpid) != 0) {
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

    @Override
    public int getItemCount() {
        return bandCommentBeanArrayList.size();
    }

    public void addData(List<BandCommentBean> bannedCommentBeans) {
        Collections.reverse(bannedCommentBeans);
        bandCommentBeanArrayList.addAll(0,bannedCommentBeans);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        TextView txv_comment, txv_band_type, txv_info, txv_date;
        ImageView imgv_band_type;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            txv_comment = itemView.findViewById(R.id.txv_comment_content);
            txv_band_type = itemView.findViewById(R.id.txv_band_type);
            txv_info = itemView.findViewById(R.id.txv_info);
            txv_date = itemView.findViewById(R.id.txv_date);
            imgv_band_type = itemView.findViewById(R.id.img_band_type);
        }
    }
}
