package com.conclearn;

import java.util.Objects;

/**
 * @author GregZQ
 * @create 2019-02-06 18:42
 * @des:  通过原生notify、wait实现一个有界阻塞队列
 */
class BlockQueue<T>{
    private int tail,head,count;
    private Object lock =  new Object();
    private int size = 16;
    private T [] list;
    public BlockQueue(int size){
        list = (T[]) new Object[size];
    }
    public BlockQueue(){
        list = (T[]) new Object[size];
    }
    public  void add(T t) throws InterruptedException {
        synchronized(lock) {
            System.out.println("添加");
            while (count == size)//谓词条件循环等待，防止信号量丢失等情况
                lock.wait();
            list[tail++]=t;
            count++;
        }
    }
    public T take() throws InterruptedException {
        synchronized (lock){
            while (count == 0)//循环等待，防止信号量丢失等情况
                lock.wait();
            T x= list[head];
            head++;
            if (head == list.length){
                head=0;
            }
            count--;
            lock.notifyAll();
            return x;
        }
    }

}
public class WaitNotifyUse {
    public static void main(String args[]) throws InterruptedException {
        BlockQueue<Integer> blockQueue = new BlockQueue<>();
        for (int i=0;i<10;i++){
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        blockQueue.add(finalI);
                    } catch (InterruptedException e) {}
                }
            }).start();
        }
        for (int i=0;i<10;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println(blockQueue.take());
                    } catch (InterruptedException e) {}
                }
            }).start();
        }
        Thread.sleep(3000);
    }
}
