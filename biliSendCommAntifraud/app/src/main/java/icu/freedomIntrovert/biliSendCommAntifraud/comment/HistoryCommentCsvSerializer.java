package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import com.alibaba.fastjson.JSON;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;

public class HistoryCommentCsvSerializer {
    private static final String[] header_banned = new String[]{"rpid", "oid", "sourceId", "comment",
            "bannedType", "commentAreaType", "checkedArea", "date"};
    private static final String[] header_before_v500 = new String[]{"oid", "sourceId", "type", "rpid",
            "parent", "root", "comment", "date", "like", "replyCount", "state", "lastCheckDate"};
    private static final String[] header_after_v500 = new String[]{"rpid", "parent", "root", "oid",
            "area_type", "source_id", "comment", "like", "reply", "last_state", "last_check_date",
            "date", "checked_area", "first_state", "pictures", "sensitive_scan_result"};
    private static final String[] header_v600 = new String[]{"rpid", "parent", "root", "oid",
            "area_type", "source_id", "comment", "like", "reply", "last_state", "last_check_date",
            "date", "checked_area", "first_state", "pictures", "sensitive_scan_result", "uid"};
    private static final String[] header_v625 = new String[]{"rpid", "parent", "root", "oid",
            "area_type", "source_id", "comment", "like", "reply", "last_state", "last_check_date",
            "date", "checked_area", "first_state", "pictures", "sensitive_scan_result", "uid","appeal_state"};

    public static List<HistoryComment> readCSVToHistoryComments(CSVReader csvReader) throws CsvValidationException, IOException {

        String[] header = csvReader.readNext();
        if (Arrays.equals(header_banned, header)) {
            return toHistoryComments(1, csvReader);
        } else if (Arrays.equals(header_before_v500, header)) {
            return toHistoryComments(2, csvReader);
        } else if (Arrays.equals(header_after_v500, header)) {
            return toHistoryComments(3, csvReader);
        } else if (Arrays.equals(header_v600, header)) {
            return toHistoryComments(4, csvReader);
        } else if (Arrays.equals(header_v625,header)){
            return toHistoryComments(5,csvReader);
        }
        return null;
    }

    private static List<HistoryComment> toHistoryComments(int headerId, CSVReader csvReader) throws CsvValidationException, IOException {
        List<HistoryComment> historyCommentList = new ArrayList<>();
        String[] data;
        while ((data = csvReader.readNext()) != null) {
            historyCommentList.add(toHistoryComment(headerId, data));
        }
        return historyCommentList;
    }

    //逐步式CSV升级，参考SQLiteOpenHelper的onUpgrade
    private static HistoryComment toHistoryComment(int headerId, String[] data) {
        String[] cache;

        //因这被Ban评论列表与历史评论列表同时存在了一段时间，两张表进行升级时合并成新的历史评论列表
        //所以 header_banned -> header_after_v500
        if (headerId == 1) { // header_banned
            cache = new String[header_after_v500.length];

            // 按照 header_banned 的字段顺序直接映射到 header_after_v500 的对应位置
            cache[0] = data[0]; // rpid
            cache[1] = "0";    // parent -> 老数据没有，留空
            cache[2] = "0";    // root -> 老数据没有，留空
            cache[3] = data[1]; // oid
            cache[4] = data[5]; // area_type -> commentAreaType
            cache[5] = data[2]; // source_id
            cache[6] = data[3]; // comment
            cache[7] = "0";    // like -> 老数据没有
            cache[8] = "0";    // reply -> 老数据没有
            cache[9] = data[4];// last_state -> bannedType
            cache[10] = data[7];   // last_check_date -> date
            cache[11] = data[7]; // date
            cache[12] = data[6]; // checked_area
            cache[13] = data[4];  // first_state -> bannedType
            cache[14] = null;   // pictures -> 老数据没有
            cache[15] = null;   // sensitive_scan_result -> 老数据没有

            //bannedType和state不一样
            if (cache[13].equals("shadowBanRecking")){
                cache[13] = "normal";
            }
            if (cache[13].equals("quickDelete")){
                cache[13] = "deleted";
            }
            if (cache[9].equals("shadowBanRecking")){
                cache[9] = "shadowBan";
            }
            if (cache[9].equals("quickDelete")){
                cache[9] = "deleted";
            }
            /*
            if (comment.firstState.equals("shadowBanRecking")) {
                comment.firstState = HistoryComment.STATE_NORMAL;
            }
            if (comment.firstState.equals("quickDelete")) {
                comment.firstState = HistoryComment.STATE_DELETED;
            }
            if (comment.lastState.equals("shadowBanRecking")) {
                comment.lastState = HistoryComment.STATE_SHADOW_BAN;
            }
            if (comment.lastState.equals("quickDelete")) {
                comment.lastState = HistoryComment.STATE_DELETED;
            }
             */
            // 跳过 header_before_v500 直接进入 header_after_v500 的逻辑
            data = cache;
            headerId = 3; // 标记为 header_after_v500
        }

        switch (headerId) {
            case 2: // header_before_v500 -> header_after_v500
                cache = new String[header_after_v500.length];
                // 按照 header_before_v500 进行升级逻辑
                cache[0] = data[3];  // rpid
                cache[1] = data[4];  // parent
                cache[2] = data[5];  // root
                cache[3] = data[0];  // oid
                cache[4] = data[2];  // area_type -> type
                cache[5] = data[1];  // source_id
                cache[6] = data[6];  // comment
                cache[7] = data[8];  // like
                cache[8] = data[9];  // reply
                cache[9] = data[10]; // last_state -> state
                cache[10] = data[11]; // last_check_date -> lastCheckDate
                cache[11] = data[7]; // date
                cache[12] = "0";    // checked_area -> 老数据没有
                cache[13] = null;    // first_state -> 老数据没有
                cache[14] = null;    // pictures -> 老数据没有
                cache[15] = null;    // sensitive_scan_result -> 老数据没有
                data = cache;

            case 3: // header_after_v500 -> header_v600
                cache = new String[header_v600.length];
                System.arraycopy(data, 0, cache, 0, data.length); // 直接拷贝数据
                cache[16] = "0";  // uid -> 老数据没有
                data = cache;

            case 4: // header_v600
            case 5: // header_v625
                return new HistoryComment(
                        new CommentArea(
                                Long.parseLong(data[3]), // oid
                                data[5],                // source_id
                                Integer.parseInt(data[4])  // type (默认值)
                        ),
                        Long.parseLong(data[0]),  // rpid
                        Long.parseLong(data[1]),  // parent
                        Long.parseLong(data[2]),  // root
                        data[6],                  // comment
                        new Date(Long.parseLong(data[11])), // date (时间戳)
                        Integer.parseInt(data[7]), // like
                        Integer.parseInt(data[8]), // replyCount
                        data[9],                  // lastState
                        new Date(Long.parseLong(data[10])), // lastCheckDate (时间戳)
                        Integer.parseInt(data[12]), // checkedArea
                        data[13],                  // firstState
                        data[14],                  // pictures
                        JSON.parseObject(data[15], SensitiveScanResult.class), // sensitiveScanResult
                        Long.parseLong(data[16]),   // uid
                        Integer.parseInt(data[17])  // appeal_state
                );
            default:
                throw new IllegalArgumentException("Invalid headerId " + headerId);
        }
        // 根据 data 构建并返回 HistoryComment 对象，这部分你可以自行补充
    }

    public static String[] getLatestHeader(){
        return header_v625;
    }

    public static String[] toCsvData(HistoryComment comment){
        return new String[]{
                String.valueOf(comment.rpid),
                String.valueOf(comment.parent),
                String.valueOf(comment.root),
                String.valueOf(comment.commentArea.oid),
                String.valueOf(comment.commentArea.type),
                comment.commentArea.sourceId,
                comment.comment,
                String.valueOf(comment.like),
                String.valueOf(comment.replyCount),
                comment.lastState,
                String.valueOf(comment.lastCheckDate.getTime()), // Assuming lastCheckDate is stored as milliseconds
                String.valueOf(comment.date.getTime()), // Assuming date is stored as milliseconds
                String.valueOf(comment.checkedArea),
                comment.firstState,
                comment.pictures,
                comment.sensitiveScanResult != null ? JSON.toJSONString(comment.sensitiveScanResult) : null,
                String.valueOf(comment.uid),
                String.valueOf(comment.appealState)
        };
    }

}
