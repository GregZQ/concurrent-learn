package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-07 16:43
 * @des: 通过join模拟多个线程协调执行
 *  模拟生产啤酒的场景
 *  先生产瓶子->装酒->包装
 */
class WinBottleRunnable implements Runnable{

    @Override
    public void run() {
        System.out.println("我在生产酒瓶");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
        System.out.println("生产酒瓶完成");
    }
}
class LoadingWineRunnable implements Runnable{
    public Thread winBottleThread;

    public LoadingWineRunnable(Thread winBottleThread){
        this.winBottleThread = winBottleThread;
    }

    @Override
    public void run() {
        try {
            winBottleThread.join();
            System.out.println("我在装酒");
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
        System.out.println("装酒完成");
    }
}
class PackWineRunnable implements Runnable{
    Thread loadingWineThread;

    public PackWineRunnable(Thread loadingWineThread){
        this.loadingWineThread = loadingWineThread;
    }

    @Override
    public void run() {
        try {
            loadingWineThread.join();
            System.out.println("我在打包");
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        System.out.println("打包完成");
    }
}

public class ThreadJoinUse {
    public static void main(String args[]) throws InterruptedException {
        Thread a = new Thread(new WinBottleRunnable());

        Thread b = new Thread(new LoadingWineRunnable(a));

        Thread c=  new Thread(new PackWineRunnable(b));

        a.start();
        b.start();
        c.start();
        System.out.println("OK");

    }
}
