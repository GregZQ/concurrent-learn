package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-08 9:41
 * @des:  线程组应用
 */

public class ThreadGroupUse {

    public static void main(String args[]) throws InterruptedException {

        //main线程在启动的时候会创建一个main线程组
        System.out.println(Thread.currentThread().getThreadGroup().getName());

        //创建线程不指定线程组默认与父线程相同的线程组，这里是main线程
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public  void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        System.out.println(thread1.getThreadGroup().getName());

        //创建线程组 ,线程组创建后会自动归属到父线程所在线程组当中
        ThreadGroup threadGroup = new ThreadGroup("新的线程组");
        //获取当前线程线程组的子线程组数量
        ThreadGroup [] tg= new ThreadGroup[Thread.currentThread().getThreadGroup().activeGroupCount()];
        //将子线程组放入到tg当中
        Thread.currentThread().getThreadGroup().enumerate(tg);
        for (ThreadGroup t: tg) {
            System.out.println(t.getName());
        }

        //使用线程组的方法管理子线程
        Thread a = new Thread(threadGroup, new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("我在睡眠");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread b = new Thread(threadGroup, new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("我在睡眠");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
            }
        });

        a.start();
        b.start();
        //获取活跃的线程数
        System.out.println("活跃线程数"+threadGroup.activeCount());
        Thread.sleep(1000);
        //中断线程组当中的子线程
        threadGroup.interrupt();

    }
}
