package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-06 16:18
 * @des:  条件队列测试
 */
class  MyConditionalQueue{
    Object object = new Object();
    Object object2 = new Object();
    public String get(){
        try {
            synchronized (object2) {
                System.out.println("OK");
                object.wait();
                Thread.sleep(1000);
                return "hello";
            }
        } catch (InterruptedException e) {
            System.out.println("OK");
        }
        return null;
    }
    public void notifyMy(){
        synchronized (object2) {
            object.notifyAll();
        }
    }
}
public class ConditionalQueueUse {
    public static void main(String args[]) throws InterruptedException {
        MyConditionalQueue queue = new MyConditionalQueue();
        for (int i =0 ;i <10;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(queue.get());
                }
            }).start();
        }
        Thread.sleep(3000);
        queue.notifyMy();
    }
}
