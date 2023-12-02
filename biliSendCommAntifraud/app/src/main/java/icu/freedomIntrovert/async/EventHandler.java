package icu.freedomIntrovert.async;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

public abstract class EventHandler extends Handler {
    public EventHandler() {
        super();
    }
    public EventHandler(@NonNull Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        handleEvent(msg.what,msg.obj);
    }

    public boolean sendMessage(int what,Object obj){
        Message m = new Message();
        m.what = what;
        m.obj = obj;
        return sendMessage(m);
    }

    public abstract void handleEvent(int what,Object data);

    public void postError(Throwable th){
        post(() -> handleError(th));
    }

    public abstract void handleError(Throwable th);

}
