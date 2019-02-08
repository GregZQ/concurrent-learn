package com.conclearn;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author GregZQ
 * @create 2019-02-02 18:30
 * @des: 通过futureTask来实现同步。
 *   如果有一个计算量较大的功能。可以开启一个线程先进行计算，
 *   需要的线程在等待获取计算结果。从而在提高效率的情况下达到同步.
 *   future可以通过callable返回计算结果。callable可以作为能有返回
 *   数据的runnable
 */

public class FutureTaskUse {

    public static void main(String args[]) throws InterruptedException, ExecutionException {
        FutureTask<Integer> futureTask = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(5000);

                return 1000;
            }
        });

        Thread thread = new Thread(futureTask);
        //线程启动
        thread.start();

        //阻塞获取结果，达到同步性
        Integer result = futureTask.get();
        System.out.println(result);
    }

}
