package icu.freedomIntrovert.biliSendCommAntifraud.async;

import android.content.Context;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import icu.freedomIntrovert.async.BackstageTaskByMVP;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public class HistoryCommentSearchTask extends BackstageTaskByMVP<HistoryCommentSearchTask.EventHandler> {
    public final String searchText;
    public final Context context;
    private final Config config;
    private final StatisticsDBOpenHelper db;

    public HistoryCommentSearchTask(EventHandler uiHandler, String searchText, Context context) {
        super(uiHandler);
        this.searchText = searchText;
        this.context = context;
        config = Config.getInstance(context);
        db = StatisticsDBOpenHelper.getInstance(context);
    }

    @Override
    protected void onStart(EventHandler eventHandler) throws Throwable {
        //rpid搜索，不受排序与过滤影响
        if (searchText != null && searchText.startsWith("[rpid]:")) {
            ArrayList<HistoryComment> historyComments = new ArrayList<>();
            String sRpid = extractRpid(searchText);
            if (sRpid != null) {
                HistoryComment historyComment = db.getHistoryComment(Long.parseLong(sRpid));
                if (historyComment != null) {
                    historyComments.add(historyComment);
                }
            }
            eventHandler.onResult(historyComments);
            return;
        }
        String sortRuler;
        switch (config.getSortRuler()) {
            case Config.SORT_RULER_DATE_ASC:
                sortRuler = StatisticsDBOpenHelper.ORDER_BY_DATE_ASC;
                break;
            case Config.SORT_RULER_DATE_DESC:
                sortRuler = StatisticsDBOpenHelper.ORDER_BY_DATE_DESC;
                break;
            case Config.SORT_RULER_LIKE_DESC:
                sortRuler = StatisticsDBOpenHelper.ORDER_BY_LIKE_DESC;
                break;
            case Config.SORT_RULER_REPLY_COUNT_DESC:
                sortRuler = StatisticsDBOpenHelper.ORDER_BY_REPLY_COUNT_DESC;
                break;
            default:
                throw new RuntimeException("config error: Unknown sort rule: " + config.getSortRuler());
        }

        LinkedList<String[]> unequalFields = new LinkedList<>();

        if (!config.getFilterRulerEnableNormal()) {
            unequalFields.add(new String[]{"last_state", HistoryComment.STATE_NORMAL});
        }
        if (!config.getFilterRulerEnableShadowBan()) {
            unequalFields.add(new String[]{"last_state", HistoryComment.STATE_SHADOW_BAN});
        }
        if (!config.getFilterRulerEnableDelete()) {
            unequalFields.add(new String[]{"last_state", HistoryComment.STATE_DELETED});
        }
        if (!config.getFilterRulerEnableOther()) {
            unequalFields.add(new String[]{"last_state", HistoryComment.STATE_INVISIBLE});
            unequalFields.add(new String[]{"last_state", HistoryComment.STATE_UNDER_REVIEW});
            unequalFields.add(new String[]{"last_state", HistoryComment.STATE_SUSPECTED_NO_PROBLEM});
            unequalFields.add(new String[]{"last_state", HistoryComment.STATE_UNKNOWN});
            unequalFields.add(new String[]{"last_state", HistoryComment.STATE_SENSITIVE});
        }

        if (!config.getFilterRulerEnableType1()) {
            unequalFields.add(new String[]{"area_type", "1"});
        }

        if (!config.getFilterRulerEnableType12()) {
            unequalFields.add(new String[]{"area_type", "12"});
        }

        if (!config.getFilterRulerEnableType11()) {
            unequalFields.add(new String[]{"area_type", "11"});
        }

        if (!config.getFilterRulerEnableType17()) {
            unequalFields.add(new String[]{"area_type", "17"});
        }
        List<HistoryComment> historyComments = db.queryAllHistoryComments(unequalFields.toArray(new String[][]{}), sortRuler);
        if (TextUtils.isEmpty(searchText)) {
            eventHandler.onResult(historyComments);
            return;
        }
        if (searchText.startsWith("『") && searchText.endsWith("』")) {
            eventHandler.onResult(filterCommentsByQuotationMarks(historyComments, searchText));
            return;
        }
        if (searchText.startsWith("[date]:")) {
            Pattern pattern = Pattern.compile("\\[date]:(\\d{4}\\.\\d{2}\\.\\d{2})-(\\d{4}\\.\\d{2}\\.\\d{2})");
            // Match the pattern against the text
            Matcher matcher = pattern.matcher(searchText);
            // Check if the text matches the pattern
            if (matcher.find()) {
                String startDateStr = matcher.group(1);
                String endDateStr = matcher.group(2);

                try {
                    // Parse start and end dates
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                    if (startDateStr != null && endDateStr != null) {
                        Date startDate = dateFormat.parse(startDateStr);
                        Date endDate = dateFormat.parse(endDateStr);
                        // Filter comments within the time range
                        eventHandler.onResult(filterCommentsWithinRange(historyComments, startDate, endDate));
                    } else {
                        eventHandler.onMatchError("解析日期时出错，");
                    }
                    // Do something with filteredComments
                } catch (ParseException e) {
                    eventHandler.onMatchError("解析日期时出错");
                }
            } else {
                eventHandler.onMatchError("格式不正确，正确示例：\n[date]:2023.06.04-2023.10.24");
            }
        }
        LinkedList<HistoryComment> searchedCommentList = new LinkedList<>();
        for (HistoryComment historyComment : historyComments) {
            if (historyComment.comment.contains(searchText) || historyComment.commentArea.sourceId.equals(searchText)) {
                searchedCommentList.add(historyComment);
            }
        }
        eventHandler.onResult(searchedCommentList);
    }

    private static String extractRpid(String input) {
        // 使用正则表达式匹配 [rpid]:后面的数字
        Pattern pattern = Pattern.compile("\\[rpid]:(\\d+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1); // 返回第一个捕获组，即 rpid 的值
        }

        return null; // 如果没有匹配到，返回 null
    }

    /**
     * 『』号匹配评论，专门系统通知解决弱智打码问题。代码由ChatGPT生成
     *
     * @param searchText 输入样例："『我*****马』"，"『我*马』"，匹配到："我超市里的马"。输入应保证"『"开头"』"结尾，*号非码去单字符，而是可变长度
     *                   也支持『回复*****』『*批』
     */
    public static List<HistoryComment> filterCommentsByQuotationMarks(List<HistoryComment> historyCommentList, String searchText) {
        List<HistoryComment> filteredComments = new ArrayList<>();

        // 构建正则表达式，将『』内的连续*号替换为正则中的.*，使一个或多个*都匹配任意字符
        String regex = searchText
                .replace("『", "")      // 去除开头的『
                .replace("』", "")      // 去除结尾的』
                .replaceAll("\\*+", ".*"); // 将一个或多个*替换为正则的.*

        // 完整正则，加上^和$以确保匹配整个内容
        regex = "^" + regex + "$";
        System.out.println("『』号匹配评论，正则表达式：" + regex);
        for (HistoryComment historyComment : historyCommentList) {
            String comment = historyComment.comment;
            // 如果评论内容与生成的正则表达式匹配，则添加到结果列表中
            if (comment.matches(regex)) {
                filteredComments.add(historyComment);
            }
        }

        return filteredComments;
    }

    public static List<HistoryComment> filterCommentsWithinRange
            (List<HistoryComment> historyCommentList, Date startDate, Date endDate) {
        List<HistoryComment> filteredComments = new LinkedList<>();
        for (HistoryComment comment : historyCommentList) {
            if (comment.date.after(startDate) && comment.date.before(endDate)) {
                filteredComments.add(comment);
            }
        }
        return filteredComments;
    }


    public interface EventHandler extends BackstageTaskByMVP.BaseEventHandler {
        void onResult(List<HistoryComment> historyComments);

        void onMatchError(String errorMsg);
    }
}
