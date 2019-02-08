package com.conclearn;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author GregZQ
 * @create 2019-02-03 10:48
 * @des: 通过Future可以阻塞获取运行任务结果
 */
class MyThread implements Runnable{
    @Override
    public void run() {
        System.out.println("OK");
    }
}
public class FutureUse {

    public static void main(String args[]) throws ExecutionException, InterruptedException {
        ExecutorService executors = Executors.newFixedThreadPool(3);

        Future future = executors.submit(new MyThread());
        System.out.println(future.get());
    }
}
