package icu.freedomIntrovert.async;

import icu.freedomIntrovert.biliSendCommAntifraud.async.comment.CommentCheckTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.comment.DialogCommentCheckEventHandler;

public class Test {
    void main(){
        CommentCheckTask commentCheckTask = new CommentCheckTask(new DialogCommentCheckEventHandler());
        TaskManger.execute(commentCheckTask);
    }
}
