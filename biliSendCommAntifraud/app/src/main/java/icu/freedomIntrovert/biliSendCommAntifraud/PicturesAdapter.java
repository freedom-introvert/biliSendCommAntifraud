package icu.freedomIntrovert.biliSendCommAntifraud;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.wgw.photo.preview.IndicatorType;
import com.wgw.photo.preview.PhotoPreview;
import com.wgw.photo.preview.interfaces.IFindThumbnailView;
import com.wgw.photo.preview.interfaces.ImageLoader;
import com.wgw.photo.preview.interfaces.OnLongClickListener;

import java.io.File;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.picturestorage.PictureLoader;
import icu.freedomIntrovert.biliSendCommAntifraud.picturestorage.PictureStorage;

public class PicturesAdapter extends RecyclerView.Adapter<PicturesAdapter.ViewHolder> {
    HistoryCommentActivity context;
    List<Comment.PictureInfo> pictureInfos;
    LinearLayoutManager linearLayoutManager;

    public PicturesAdapter(HistoryCommentActivity context, List<Comment.PictureInfo> pictureInfos, LinearLayoutManager linearLayoutManager) {
        this.context = context;
        this.pictureInfos = pictureInfos;
        this.linearLayoutManager = linearLayoutManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_pictrues, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageView imageView = holder.imageView;
        PictureLoader.with(context).load(pictureInfos.get(position).img_src).into(imageView);
     //   Glide.with(context).load(pictureInfos.get(position).img_src).into(imageView);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoPreview.with(context)
                        .sources(pictureInfos)
                        .indicatorType(IndicatorType.TEXT)
                        .showThumbnailViewMask(true)
                        .fullScreen(true)
                        .animDuration(400L)
                        .defaultShowPosition(holder.getLayoutPosition())
                        .imageLoader(new ImageLoader() {
                            @Override
                            public void onLoadImage(int position, @Nullable Object source, @NonNull ImageView imageView) {
                                PictureLoader.with(context).load(pictureInfos.get(position).img_src).into(imageView);
                            }
                        })
                        .onPageChangeListener(new ViewPager.OnPageChangeListener() {
                            @Override
                            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                            }

                            @Override
                            public void onPageSelected(int position) {
                                linearLayoutManager.scrollToPosition(position);
                            }

                            @Override
                            public void onPageScrollStateChanged(int state) {

                            }
                        })
                        .onLongClickListener(new OnLongClickListener() {
                            @Override
                            public boolean onLongClick(int position, FrameLayout customViewRoot, ImageView imageView) {
                                String imgSrc = pictureInfos.get(position).img_src;
                                File pictureFile = PictureStorage.getPictureFile(context, imgSrc);
                                //启动系统文件管理器另存为图片
                                context.savePicFileLauncher.launch(pictureFile);
                                return false;
                            }
                        })
                        .build()
                        .show(new IFindThumbnailView() {
                            @Override
                            public View findView(int position) {
                                return linearLayoutManager.findViewByPosition(position).findViewById(R.id.picture);
                            }
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return pictureInfos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.picture);
        }
    }
}
