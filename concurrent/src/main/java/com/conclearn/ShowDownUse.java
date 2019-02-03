package com.conclearn;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author GregZQ
 * @create 2019-02-03 15:40
 * @des:  线程池关闭
 */
class Task1 implements Callable{

    @Override
    public Object call() throws Exception {
        System.out.println("短时间任务");
        Thread.sleep(1000);
        return null;
    }
}
class Task2 implements Callable{

    @Override
    public Object call() throws Exception {
        System.out.println("长时间任务");
        Thread.sleep(5000);
        return null;
    }
}
public class ShowDownUse {

    public static void main(String args[]) throws InterruptedException {
        ExecutorService executors= Executors.newFixedThreadPool(3);
        executors.submit(new Task1());
        executors.submit(new Task2());
        executors.submit(new Task1());


        executors.shutdown();
        while(!executors.awaitTermination(1, TimeUnit.SECONDS)){
            System.out.println("尚未关闭");
        }
        System.out.println("已经关闭");
    }
}
