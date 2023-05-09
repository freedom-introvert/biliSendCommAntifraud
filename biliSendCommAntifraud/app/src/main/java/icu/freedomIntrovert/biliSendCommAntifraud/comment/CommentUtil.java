package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.VoidDialogInterfaceOnClickListener;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.CommentPresenter;

public class CommentUtil {
    private CommentArea yourCommentArea;
    private String sourceRandomComments;
    private String[] randomComments;
    private HashMap<CommentArea, LinkedList<String>> usedTestCommentMap;
    SharedPreferences sp_config;

    public CommentUtil(SharedPreferences sp_config) {
        this.sp_config = sp_config;
        this.sourceRandomComments = sp_config.getString("random_comments", "日照香炉生紫烟\n" +
                "遥看瀑布挂前川\n" +
                "飞流直下三千尺\n" +
                "疑是银河落九天\n" +
                "床前明月光\n" +
                "疑是地上霜\n" +
                "举头望明月\n" +
                "低头思故乡\n" +
                "横看成岭侧成峰\n" +
                "远近高低各不同 \n" +
                "不识庐山真面目\n" +
                "只缘身在此山中");
        this.randomComments = sourceRandomComments.split("\\n");
        usedTestCommentMap = new HashMap<>();
        if (sp_config.contains("your_comment_area_oid")) {
            this.yourCommentArea = new CommentArea(Long.parseLong(sp_config.getString("your_comment_area_oid", "")), sp_config.getString("your_comment_area_sourceId", ""), sp_config.getInt("your_comment_area_type", 1));
        }
    }

    public String getSourceRandomComments() {
        return sourceRandomComments;
    }

    public String getAreaSourceText() {
        return sp_config.getString("your_comment_area_sourceText", "");
    }

    public CommentArea getYourCommentArea() {
        return yourCommentArea;
    }

    public boolean setYourCommentArea(String sourceAreaText, CommentManipulator commentManipulator) throws IOException {
        CommentArea commentArea = commentManipulator.matchCommentArea(sourceAreaText);
        if (commentArea != null) {
            sp_config.edit().putString("your_comment_area_oid", String.valueOf(commentArea.oid))
                    .putInt("your_comment_area_type", commentArea.areaType)
                    .putString("your_comment_area_sourceId", commentArea.sourceId)
                    .putString("your_comment_area_sourceText", sourceAreaText)
                    .apply();
            yourCommentArea = commentArea;
            return true;
        } else {
            return false;
        }
    }

    public void setYourCommentArea(Context context, CommentPresenter commentPresenter){
        View dialogView = View.inflate(context, R.layout.edit_text, null);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.setText(getAreaSourceText());
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("你的评论区（你是up主），用于检测查重黑名单与扫描敏感词").setView(dialogView).setNegativeButton("取消", new VoidDialogInterfaceOnClickListener()).setPositiveButton("设置", null).show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commentPresenter.matchToArea(editText.getText().toString(), new CommentPresenter.MatchToAreaCallBack() {
                    @Override
                    public void onNetworkError(Throwable th) {
                        Toast.makeText(context,"网络错误：" + th.getMessage(),Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onMatchedArea(CommentArea commentArea) {
                        if (commentArea != null) {
                            sp_config.edit().putString("your_comment_area_oid", String.valueOf(commentArea.oid))
                                    .putInt("your_comment_area_type", commentArea.areaType)
                                    .putString("your_comment_area_sourceId", commentArea.sourceId)
                                    .putString("your_comment_area_sourceText", editText.getText().toString())
                                    .apply();
                            yourCommentArea = commentArea;
                            dialog.dismiss();
                            Toast.makeText(context, "设置成功！", Toast.LENGTH_SHORT).show();
                        } else {
                            editText.setError("输入的内容未解析到评论区！");
                        }
                    }
                });
            }
        });
    }

    public String getRandomComment(CommentArea area) {
        //获取随机测试评论，并且保证同一个评论区不会发布相同的评论
        if (usedTestCommentMap.get(area) == null) {
            LinkedList<String> noUsedCommentList = new LinkedList<>(Arrays.asList(randomComments));
            usedTestCommentMap.put(area, noUsedCommentList);
        }
        Random random = new Random();
        LinkedList<String> noUsedCommentList = usedTestCommentMap.get(area);
        int randomNum = random.nextInt(noUsedCommentList.size());
        String randomComment = noUsedCommentList.get(randomNum);
        noUsedCommentList.remove(randomNum);
        Log.i("randomComment", randomComment);
        return randomComment;
    }

    public void updateRandomComments(String sourceRandomComments) {
        this.sourceRandomComments = sourceRandomComments;
        sp_config.edit().putString("random_comments", sourceRandomComments).apply();
    }

    public static String sourceIdToUrl(CommentArea area){
        String url = null;
        if (area.areaType == CommentArea.AREA_TYPE_VIDEO) {
            url = "https://www.bilibili.com/video/" + area.sourceId;
        } else if (area.areaType == CommentArea.AREA_TYPE_ARTICLE) {
            url = "https://www.bilibili.com/read/" + area.sourceId;
        } else if (area.areaType == CommentArea.AREA_TYPE_DYNAMIC11 || area.areaType == CommentArea.AREA_TYPE_DYNAMIC17) {
            url = "https://t.bilibili.com/" + area.sourceId;
        }
        return url;
    }

    public static String subComment(String comment, int length) {
        if (comment.length() > length) {
            return comment.substring(0, length - 2) + "……";
        } else {
            return comment;
        }
    }



}
