package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;

public class MartialLawCommentAreaListAdapter extends RecyclerView.Adapter<MartialLawCommentAreaListAdapter.ViewHolder> {
    ArrayList<MartialLawCommentArea> areaArrayList;
    Context context;
    private StatisticsDBOpenHelper statisticsDBOpenHelper;

    public MartialLawCommentAreaListAdapter(ArrayList<MartialLawCommentArea> areaArrayList, Context context) {
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        this.areaArrayList = areaArrayList;
        Collections.reverse(this.areaArrayList);
        this.context = context;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = View.inflate(context, R.layout.item_martial_law_comment_area, null);
        return new ViewHolder(itemView);
    }

    public void addData(List<MartialLawCommentArea> areaArrayList){
        Collections.reverse(areaArrayList);
        this.areaArrayList.addAll(0,areaArrayList);
        notifyDataSetChanged();
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MartialLawCommentArea area = areaArrayList.get(position);
        //根据oid从数据库中加载图片
        byte[] coverImageData = statisticsDBOpenHelper.selectMartialLawCommentAreaCoverImage(area.oid);
        if (area.areaType == CommentArea.AREA_TYPE_DYNAMIC11 || area.areaType == CommentArea.AREA_TYPE_DYNAMIC17) {
            holder.cover_image.setImageDrawable(context.getDrawable(R.drawable.dynmic));
        } else {
            //动态图片我画的，因为动态没有封面
            holder.cover_image.setImageBitmap(BitmapFactory.decodeByteArray(coverImageData, 0, coverImageData.length));
        }
        holder.txv_title.setText(area.title);
        holder.txv_up.setText("UP:" + area.up);
        holder.txv_source_id_ia.setText(area.sourceId);


        String areaType = null;
        switch (area.areaType) {
            case CommentArea.AREA_TYPE_VIDEO:
                areaType = "视频";
                holder.img_area_type.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_smart_display_24));
                break;
            case CommentArea.AREA_TYPE_ARTICLE:
                areaType = "专栏";
                holder.img_area_type.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_art_track_24));
                break;
            case CommentArea.AREA_TYPE_DYNAMIC11:
            case CommentArea.AREA_TYPE_DYNAMIC17:
                areaType = "动态";
                holder.img_area_type.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_insert_chart_24));
                break;
        }
        String defaultDisposalMethod = null;
        switch (area.defaultDisposalMethod) {
            case BandCommentBean.BANNED_TYPE_SHADOW_BAN:
                holder.img_band_type.setImageDrawable(context.getDrawable(R.drawable.hide));
                defaultDisposalMethod = "发评默认仅自己可见";
                break;
            case BandCommentBean.BANNED_TYPE_QUICK_DELETE:
                holder.img_band_type.setImageDrawable(context.getDrawable(R.drawable.deleted));
                defaultDisposalMethod = "发评立即被系统删除";
                break;
        }
        holder.txv_default_disposal_method.setText(defaultDisposalMethod);
        String finalDefaultDisposalMethod = defaultDisposalMethod;
        String finalAreaType = areaType;
        holder.itemView.setOnClickListener(v -> {
            View dialogView = View.inflate(context, R.layout.dialog_martial_law_comment_area_info, null);
            TextView txv_title = dialogView.findViewById(R.id.txv_title);
            TextView txv_up = dialogView.findViewById(R.id.txv_up);
            TextView txv_oid = dialogView.findViewById(R.id.txv_oid);
            TextView txv_source_id = dialogView.findViewById(R.id.txv_source_id);
            TextView txv_band_type = dialogView.findViewById(R.id.txv_band_type);
            TextView txv_area_type = dialogView.findViewById(R.id.txv_area_type);
            txv_title.setText(area.title);
            txv_up.setText("UP:" + area.up);
            txv_oid.setText(area.oid);
            txv_source_id.setText(area.sourceId);
            txv_band_type.setText(finalDefaultDisposalMethod);
            txv_area_type.setText(finalAreaType);
            new AlertDialog.Builder(context)
                    .setTitle("详情")
                    .setView(dialogView)
                    .setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener())
                    .setNeutralButton("删除", (dialog12, which) -> {
                        new AlertDialog.Builder(context).setMessage("确认删除吗？")
                                .setNegativeButton("手滑了", new VoidDialogInterfaceOnClickListener())
                                .setPositiveButton("确认", (dialog14, which2) -> {
                                    if (statisticsDBOpenHelper.deleteMartialLawCommentArea(area.oid) != 0) {
                                        areaArrayList.remove(holder.getAdapterPosition());
                                        notifyItemRemoved(holder.getAdapterPosition());
                                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                                    }
                                }).show();
                    }).setNegativeButton("打开详情页", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String url = null;
                            if (area.areaType == CommentArea.AREA_TYPE_VIDEO) {
                                url = "https://www.bilibili.com/video/" + area.sourceId;
                            } else if (area.areaType == CommentArea.AREA_TYPE_ARTICLE) {
                                url = "https://www.bilibili.com/read/" + area.sourceId;
                            } else if (area.areaType == CommentArea.AREA_TYPE_DYNAMIC11 || area.areaType == CommentArea.AREA_TYPE_DYNAMIC17) {
                                url = "https://t.bilibili.com/" + area.sourceId;
                            }
                            Uri uri = Uri.parse(url);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            context.startActivity(intent);
                        }
                    }).show();
        });
    }

    @Override
    public int getItemCount() {
        return areaArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        ImageView cover_image, img_area_type, img_band_type;
        TextView txv_title, txv_source_id_ia, txv_up, txv_default_disposal_method;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            cover_image = itemView.findViewById(R.id.cover_image);
            img_area_type = itemView.findViewById(R.id.img_area_type);
            img_band_type = itemView.findViewById(R.id.img_band_type);
            txv_title = itemView.findViewById(R.id.txv_title);
            txv_source_id_ia = itemView.findViewById(R.id.txv_source_id_ia);
            txv_up = itemView.findViewById(R.id.txv_up);
            txv_default_disposal_method = itemView.findViewById(R.id.txv_default_disposal_method);
        }
    }
}
