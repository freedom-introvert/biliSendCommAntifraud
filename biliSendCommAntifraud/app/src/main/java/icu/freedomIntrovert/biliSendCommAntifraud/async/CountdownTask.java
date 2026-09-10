package icu.freedomIntrovert.biliSendCommAntifraud.async;

import icu.freedomIntrovert.async.BackstageTaskByMVP;

public class CountdownTask extends BackstageTaskByMVP<CountdownTask.EventHandler> {

    public final int waitSeconds;

    public CountdownTask(int waitSeconds, EventHandler handler) {
        super(handler);
        this.waitSeconds = waitSeconds;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        for (int i = 0; i < waitSeconds; i++) {
            Thread.sleep(1000);
            handler.onProgress(waitSeconds,i);
        }
        handler.onComplete(this);
    }

    public interface EventHandler extends BackstageTaskByMVP.BaseEventHandler {
        void onProgress(int max,int progress);
        void onComplete(CountdownTask task);
    }
}
