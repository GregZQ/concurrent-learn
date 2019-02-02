package com.conclearn;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @author GregZQ
 * @create 2019-02-02 19:36
 * @des: 含义：多个线程互相等待，当所有
 * 线程准备完毕后继续向下执行
 */
class Runner implements Runnable{
    private String name ;
    private CyclicBarrier cyclicBarrier;

    public Runner(String name, CyclicBarrier barrier){
        this.name = name;
        this.cyclicBarrier = barrier;
    }

    @Override
    public void run() {
        System.out.println(name +" 已经准备 ");
        try {
            cyclicBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        System.out.println(name+ " go! ");
    }
}
public class CyclicBarrierUse {

    private static CyclicBarrier cyclicBarrier= new CyclicBarrier(5);

    public static void main(String args[]){
        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);
        for (int i =0 ;i<10;i++){
            new Thread(new Runner(i+"",cyclicBarrier)).start();
        }

    }
}
