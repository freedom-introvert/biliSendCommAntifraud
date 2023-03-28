package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

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
            this.yourCommentArea = new CommentArea(sp_config.getString("your_comment_area_oid", ""), sp_config.getString("your_comment_area_sourceId", ""), sp_config.getInt("your_comment_area_type", 1));
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
            sp_config.edit().putString("your_comment_area_oid", commentArea.oid)
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

    public boolean checkAreaMartialLaw(CommentManipulator commentManipulator, CommentArea commentArea) {
        return false;
    }

    public static String subComment(String comment, int length) {
        if (comment.length() > length) {
            return comment.substring(0, length - 2) + "……";
        } else {
            return comment;
        }
    }


}
