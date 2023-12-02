package icu.freedomIntrovert.biliSendCommAntifraud.async.comment;

import icu.freedomIntrovert.async.BackstageTask;

public class CommentCheckTask extends BackstageTask<CommentCheckEventHandler> {

    public CommentCheckTask(CommentCheckEventHandler handle) {
        super(handle);
    }

    @Override
    protected void start(CommentCheckEventHandler eventHandler) {
        eventHandler.sendEmptyMessage(CommentCheckEventHandler.WHAT_RESULT_OK);
    }
}
