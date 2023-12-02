package icu.freedomIntrovert.async;

public abstract class BackstageTask<T extends  EventHandler> implements Runnable{
    private final T handle;
    public BackstageTask(T handle){
        this.handle = handle;
    }

    @Override
    public void run(){
        start(handle);
    }

    protected abstract void start(T eventHandler);
}
