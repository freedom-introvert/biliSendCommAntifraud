package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class RandomComments {
    private final Config config;
    private static RandomComments instance;
    private String sourceRandomComments;
    private String[] randomComments;
    private final HashMap<CommentArea, LinkedList<String>> usedTestCommentMap;

    private RandomComments(Config config){
        this.config = config;
        sourceRandomComments = config.getRandomComments();
        this.randomComments = sourceRandomComments.split("\\n");
        usedTestCommentMap = new HashMap<>();
    }

    public synchronized static RandomComments getInstance(Context context){
        if(instance == null){
            instance = new RandomComments(Config.getInstance(context));
        }
        return instance;
    }

    public String getRandomComment(CommentArea area) {
        //获取随机测试评论，并且保证同一个评论区不会发布相同的评论
        if (usedTestCommentMap.get(area) == null) {
            LinkedList<String> noUsedCommentList = new LinkedList<>(Arrays.asList(randomComments));
            usedTestCommentMap.put(area, noUsedCommentList);
        }
        Random random = new Random();
        LinkedList<String> noUsedCommentList = usedTestCommentMap.get(area);
        assert noUsedCommentList != null;
        int randomNum = random.nextInt(noUsedCommentList.size());
        String randomComment = noUsedCommentList.get(randomNum);
        noUsedCommentList.remove(randomNum);
        Log.i("randomComment", randomComment);
        return randomComment;
    }

    public String getSourceRandomComments() {
        return sourceRandomComments;
    }

    public void updateRandomComments(String sourceRandomComments){
        this.sourceRandomComments = sourceRandomComments;
        config.setRandomComments(sourceRandomComments);
        this.randomComments = sourceRandomComments.split("\\n");
    }

}
