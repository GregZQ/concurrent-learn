package com.conclearn;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author GregZQ
 * @create 2019-02-03 16:24
 * @des: 用途： ..k，可以交给
 * UncaughtExceptionHandlerUse进行捕获
 */
class  MyUncaughtException implements Thread.UncaughtExceptionHandler{

    //也可以将其追加到日志当中去
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.out.println("线程名称为:"+t.getName()+"，抛出异常未"+e.getMessage());
    }
}
public class UncaughtExceptionHandlerUse {

    public static void main(String args[]){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int i =1/0;
            }
        };

        Thread thread = new Thread(runnable);
        thread.setUncaughtExceptionHandler(new MyUncaughtException());
        thread.start();
    }
}
