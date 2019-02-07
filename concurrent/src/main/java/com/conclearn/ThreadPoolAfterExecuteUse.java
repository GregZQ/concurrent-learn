package com.conclearn;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author GregZQ
 * @create 2019-02-07 21:20
 * @des:
 */
class MyThreadPoolExecutor extends ThreadPoolExecutor{

    public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (r instanceof Runtime){
            if (Objects.nonNull(t)){
                System.out.println(t);
            }
        }else if (r instanceof FutureTask){
            FutureTask futureTask = (FutureTask) r;
            try {
                futureTask.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                //处理捕获的异常
            }
        }
    }
}
public class ThreadPoolAfterExecuteUse {
}
