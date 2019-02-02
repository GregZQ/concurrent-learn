package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-02 17:34
 * @des: 线程打断
 */
public class ThreadInterrupt {

    public static void main(String args[]) throws InterruptedException {
        Thread thread = new Thread(() -> {
            while (true) {
                // 响应中断
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Java技术栈线程被中断，程序退出。");
                    return;
                }

                try {
                    Thread.sleep(3000);
                    System.out.println("OK");
                } catch (InterruptedException e) {
                    System.out.println("Java技术栈线程休眠被中断，程序退出。");
                    //Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
        Thread.sleep(2000);
        thread.interrupt();
    }
}
