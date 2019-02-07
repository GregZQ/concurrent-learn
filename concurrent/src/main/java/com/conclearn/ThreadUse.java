package com.conclearn;

import java.util.concurrent.*;

/**
 * @author GregZQ
 * @create 2019-02-07 14:38
 * @des:  线程创建的几种方式
 */
//1.集成thread，重写run方法
class  MyThread1 extends Thread{
    int count =0;
    @Override
    public void run() {
        System.out.println("继承thread");
        count();
    }

    public void count(){
        count ++;
        count();
    }
}
//2. 实现runnable接口
class MyRunnable implements Runnable{
    @Override
    public void run() {
        System.out.println("实现runnable接口");
    }
}
//3.实现callable接口
class MyCallable implements Callable<String>{

    @Override
    public String call() throws Exception {
        System.out.println("实现callable接口");
        return "cllable";
    }
}
public class ThreadUse {

    public static void main(String args[]) throws ExecutionException, InterruptedException {
        //通过继承Thread
        MyThread1 myThread1 = new MyThread1();
        myThread1.start();

        //通过实现runnable方式
        MyRunnable myRunnable = new MyRunnable();
        new Thread(myRunnable).start();

        //通过实现callable接口
        MyCallable myCallable = new MyCallable();
        FutureTask<String> futureTask = new FutureTask<String>(myCallable);
        new Thread(futureTask).start();
        System.out.println(futureTask.get());

        //通过使用线程池
        ExecutorService executors= Executors.newFixedThreadPool(3);
        Future<String> future = executors.submit(myCallable);
        System.out.println(future.get());
        executors.execute(myRunnable);
    }
}
