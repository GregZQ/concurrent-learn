package com.conclearn;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author GregZQ
 * @create 2019-02-06 19:15
 * @des: 通过Lock的condition实现一个阻塞队列
 */
class MyBlockQueue2<T>{
    private Lock lock = new ReentrantLock();
    private Condition putCon = lock.newCondition();
    private Condition takeCon = lock.newCondition();
    private T [] list;
    private int tail,head,count;
    private int size;
    public MyBlockQueue2(){
        size =16;
        list = (T[]) new Object[size];
    }
    public void put(T t) throws InterruptedException {
        lock.lock();
        try{
            while (count==size){
                putCon.await();
            }
            list[tail++] = t;
            if(tail == size){
                tail =0 ;
            }
            count++;
            takeCon.signalAll();
        }finally {
            lock.unlock();
        }
    }
    public T take() throws InterruptedException {
        lock.lock();
        try{
            while (count == 0){
                takeCon.await();
            }
            T t = list[head++];
            if (head==size){
                head = 0;
            }
            count--;
            putCon.signalAll();
            return t;
        }finally {
            lock.unlock();
        }
    }
}
public class ConditionUse {
    public static void main(String args[]){
        MyBlockQueue2<Integer> myBlockQueue2 = new MyBlockQueue2();
        for (int i =0 ;i<10;i++){
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        myBlockQueue2.put(finalI);
                    } catch (InterruptedException e) {}
                }
            }).start();
        }
        for(int i =0 ;i<11;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println(myBlockQueue2.take());
                    } catch (InterruptedException e) {}
                }
            }).start();
        }
    }
}
