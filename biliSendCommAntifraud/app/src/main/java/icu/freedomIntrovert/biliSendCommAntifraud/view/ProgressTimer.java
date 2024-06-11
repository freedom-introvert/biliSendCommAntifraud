package icu.freedomIntrovert.biliSendCommAntifraud.view;

public class ProgressTimer {

    private final long TimeMs;
    private final int max;
    private final ProgressLister progressLister;

    private boolean isStopped = false;

    public ProgressTimer(long timeMs, int max, ProgressLister progressLister) {
        TimeMs = timeMs;
        this.max = max;
        this.progressLister = progressLister;
    }

    public void start() {
        long sleepSeg = TimeMs / max;
        for (int i = 0; i <= max && !isStopped; i++) {
            try {
                Thread.sleep(sleepSeg);
                progressLister.onNewProgress(i,sleepSeg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        isStopped = true;
    }

    public interface ProgressLister {
        void onNewProgress(int progress,long sleepSeg);
    }
}
