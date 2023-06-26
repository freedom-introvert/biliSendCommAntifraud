package icu.freedomIntrovert.biliSendCommAntifraud.danmaku;

import android.os.Handler;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.xml.parsers.ParserConfigurationException;

import icu.freedomIntrovert.biliSendCommAntifraud.NetworkCallBack;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public class DanmakuPresenter {
    private Handler handler;
    public DanmakuManipulator danmakuManipulator;
    public StatisticsDBOpenHelper statisticsDBOpenHelper;
    private boolean enableStatistics;
    public long waitTime;
    private Executor executor;

    public DanmakuPresenter(Handler handler, DanmakuManipulator danmakuManipulator, StatisticsDBOpenHelper statisticsDBOpenHelper, long waitTime, boolean enableStatistics) {
        this.handler = handler;
        this.danmakuManipulator = danmakuManipulator;
        this.statisticsDBOpenHelper = statisticsDBOpenHelper;
        this.enableStatistics = enableStatistics;
        this.waitTime = waitTime;
        executor = Executors.newSingleThreadExecutor();
    }

    public void checkDanmaku(long oid, long dmid, String content, String accessKey, long avid, CheckDanmakuCallBack callBack) {
        executor.execute(() -> {
            try {
                handler.post(() -> callBack.onSleeping(waitTime));
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(callBack::onGettingNoAccountDMList);
                if (danmakuManipulator.findDanmaku(oid, dmid, null)){
                    handler.post(callBack::thenOk);
                } else {
                    handler.post(callBack::onGettingHasAccountDMList);
                    if (danmakuManipulator.findDanmaku(oid, dmid, accessKey)){
                        handler.post(callBack::thenShadowBan);
                    } else {
                        handler.post(callBack::thenDeleted);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> callBack.onNetworkError(e));
            } catch (ParserConfigurationException | SAXException e) {
                e.printStackTrace();
            }
        });
    }

    public interface CheckDanmakuCallBack extends NetworkCallBack {
        void onSleeping(long waitTime);

        void onGettingHasAccountDMList();

        void onGettingNoAccountDMList();

        void thenOk();

        void thenDeleted();

        void thenShadowBan();
    }

}
