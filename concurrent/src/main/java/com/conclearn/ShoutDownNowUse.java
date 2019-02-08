package com.conclearn;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author GregZQ
 * @create 2019-02-03 15:18
 * @des: 由于ShoutDownNow 会关闭所有的线程。
 * 而对于那些正在执行的线程也会被关闭。
 *
 * 本代码可以实现获取那些运行过程当中被关闭的线程
 */
class TrackingExecutor extends AbstractExecutorService{
    private ExecutorService executorService;
    private Set<Runnable> tasksCancelledAtShutDown =
            Collections.synchronizedSet(new HashSet<>());
    public TrackingExecutor(ExecutorService executorService){
        this.executorService =executorService;
    }
    public Set<Runnable> getTasksCancelledAtShutDown(){
        return this.tasksCancelledAtShutDown;
    }
    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout,unit);
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    command.run();
                }finally {
                    if (isShutdown()&&
                            Thread.currentThread().isInterrupted()){
                        tasksCancelledAtShutDown.add(command);
                    }
                }
            }
        });
    }
}
public class ShoutDownNowUse {
    public static void main(String args[]) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        TrackingExecutor trackingExecutor = new TrackingExecutor(executorService);

        for (int i=0;i<10;i++){
            trackingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        Thread.sleep(1000);

        trackingExecutor.shutdownNow();
        int size = trackingExecutor.getTasksCancelledAtShutDown().size();

        System.out.println(size);
    }
}
