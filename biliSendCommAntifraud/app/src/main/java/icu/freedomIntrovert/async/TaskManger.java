package icu.freedomIntrovert.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManger {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static void start(Runnable runnable){
        executorService.execute(runnable);
    }

    public static void execute(BackstageTask<?> backstageTask){
        start(backstageTask);
    }

}
