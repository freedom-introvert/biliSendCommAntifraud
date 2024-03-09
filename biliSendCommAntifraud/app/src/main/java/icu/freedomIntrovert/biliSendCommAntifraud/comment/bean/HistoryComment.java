package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Date;

public class HistoryComment extends Comment {
    public static final String STATE_NORMAL = "normal";
    public static final String STATE_SHADOW_BAN = "shadowBan";
    public static final String STATE_DELETED = "deleted";
    //基于前端的隐藏，评论正常出现在JSON数据中，但是"invisible": true，只是客户端不展示
    public static final String STATE_INVISIBLE = "invisible";
    //评论shadowBan但可以获取评论列表或你是UP发的评论被shadowBan，疑似审核中，因为回复列表原因，不支持判断回复评论的疑似审核
    public static final String STATE_UNDER_REVIEW = "underReview";
    //疑似没问题
    public static final String STATE_SUSPECTED_NO_PROBLEM = "suspectedNoProblem";
    //直接去申诉所导致的未知状态
    public static final String STATE_UNKNOWN = "unknown";
    //评论区die了
    public static final String STATE_COMMENT_AREA_DIED = "commentAreaDied";
    public static final String STATE_SENSITIVE = "sensitive";
    /*
    没有检查过评论区                                     [0]
     - 正常                      --测试评论正常          [1]
     +- 没有检查过是否仅ban
        - 仅                     --同文测试评论正常       [2]
        - 全                     --同文测试评论被ban     [3]
     - 戒严                      --测试评论被ban        [4]

   当然，有《戒严评论列表》，为避免两列表维护问题，戒严评论列表仅提供快速得知是否戒严

    */
    public static final int CHECKED_NO_CHECK = 0;
    public static final int CHECKED_NOT_MARTIAL_LAW = 1;
    public static final int CHECKED_ONLY_BANNED_IN_THIS_AREA = 2;
    public static final int CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA = 3;
    public static final int CHECKED_MARTIAL_LAW = 4;
    public int like, replyCount;
    public int checkedArea;
    public String firstState;
    public String lastState;
    public Date lastCheckDate;
    public SensitiveScanResult sensitiveScanResult;

    public HistoryComment(CommentArea commentArea, long rpid, long parent, long root, String comment,
                          Date date, int like, int replyCount, String lastState, Date lastCheckDate,
                          int checkedArea, String firstState, String pictures,
                          SensitiveScanResult sensitiveScanResult) {
        super(commentArea, rpid, parent, root, comment, pictures, date);
        this.like = like;
        this.replyCount = replyCount;
        this.lastState = lastState;
        this.lastCheckDate = lastCheckDate;
        this.checkedArea = checkedArea;
        this.firstState = firstState;
        this.sensitiveScanResult = sensitiveScanResult;
    }

    //5.0.0及之前csv导入专用方法
    public HistoryComment(long oid, String sourceId, int type, long rpid, long parent, long root, String comment, Date date, int like, int replyCount, String lastState, Date lastCheckDate) {
        super(new CommentArea(oid, sourceId, type), rpid, parent, root, comment, null, date);
        this.like = like;
        this.replyCount = replyCount;
        if (lastState.equals("shadowBanRecking")) {
            lastState = STATE_SHADOW_BAN;
            firstState = STATE_NORMAL;
        }
        if (lastState.equals("quickDelete")) {
            lastState = STATE_DELETED;
        }
        this.lastState = lastState;
        this.lastCheckDate = lastCheckDate;
    }

    public HistoryComment(Comment originalComment) {
        super(originalComment.commentArea, originalComment.rpid, originalComment.parent, originalComment.root, originalComment.comment, originalComment.pictures, originalComment.date);
    }

    public String getFormatLastCheckDateFor_yMd() {
        return getFormatDateFor_yMd(lastCheckDate);
    }

    public String getFormatLastCheckDateFor_yMdHms() {
        return getFormatDateFor_yMdHms(lastCheckDate);
    }

    public void setFirstStateAndCurrentState(String state) {
        this.firstState = state;
        this.lastState = state;
    }

    public static String getStateDesc(String state) {
        if (TextUtils.isEmpty(state)) {
            return "无";
        }
        switch (state) {
            case STATE_NORMAL:
                return "评论正常";
            case STATE_SHADOW_BAN:
                return "评论仅自己可见";
            case STATE_DELETED:
                return "评论已被删除";
            case STATE_INVISIBLE:
                return "评论invisible，前端不可见";
            case STATE_UNDER_REVIEW:
                return "评论疑似审核中";
            case STATE_SUSPECTED_NO_PROBLEM:
                return "评论疑似正常（申诉提示无可申诉评论）";
            case STATE_UNKNOWN:
                return "未知（发送评论直接去申诉专属）";
            default:
                return state;
        }
    }

    @NonNull

    @Override
    public String toString() {
        return "HistoryComment{" +
                "like=" + like +
                ", replyCount=" + replyCount +
                ", checkedArea=" + checkedArea +
                ", firstState='" + firstState + '\'' +
                ", lastState='" + lastState + '\'' +
                ", lastCheckDate=" + lastCheckDate +
                ", sensitiveScanResult=" + sensitiveScanResult +
                ", commentArea=" + commentArea +
                ", rpid=" + rpid +
                ", parent=" + parent +
                ", root=" + root +
                ", comment='" + comment + '\'' +
                ", pictures='" + pictures + '\'' +
                ", date=" + date +
                '}';
    }
}
