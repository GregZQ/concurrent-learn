package com.conclearn;

import java.util.concurrent.CountDownLatch;

/**
 * @author GregZQ
 * @create 2019-02-02 18:09
 * @des:    通过它计算某个并发任务执行的时间
 */
public class CountDownLatchUse {
    private static CountDownLatch start  = new CountDownLatch(1);
    private static CountDownLatch end    = new CountDownLatch(10);

    public static void main(String args[]) throws InterruptedException {
        for (int i=0;i<10;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await();
                        Thread.sleep(1000);
                        end.countDown();
                    } catch (InterruptedException e) {} finally {

                    }
                }
            }).start();
        }
        long startTime = System.currentTimeMillis();
        start.countDown();
        end.await();
        long endTime   = System.currentTimeMillis();
        System.out.println(endTime-startTime);
    }
}
