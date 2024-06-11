package icu.freedomIntrovert.async;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;



public abstract class BackstageTaskByMVP<T extends BackstageTaskByMVP.BaseEventHandler> implements Runnable{
    private final T uiHandler;

    public BackstageTaskByMVP(T uiHandler) {
        this.uiHandler = uiHandler;
    }

    protected abstract void onStart(T eventHandlerProxy) throws Throwable;

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        T proxyInstance = (T) Proxy.newProxyInstance(uiHandler.getClass().getClassLoader(),
                uiHandler.getClass().getInterfaces(),
                new EvProxyHandler(uiHandler));
        try {
            onStart(proxyInstance);
            TaskManger.postOnUiThread(uiHandler::onComplete);
        } catch (Throwable e) {
            e.printStackTrace();
            TaskManger.postOnUiThread(() -> uiHandler.onError(e));
        }
    }


    public void execute() {
        TaskManger.start(this);
    }

    public static class EvProxyHandler implements InvocationHandler {
        Object evHandler;

        public EvProxyHandler(Object evHandler) {
            this.evHandler = evHandler;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class){
                return method.invoke(proxy,args);
            }
            TaskManger.postOnUiThread(() -> {
                try {
                    method.invoke(evHandler,args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }
    }

    public interface BaseEventHandler{
        default void onError(Throwable th) {
            throw new RuntimeException(th);
        }

        default void onComplete() {
        }
    }

}

