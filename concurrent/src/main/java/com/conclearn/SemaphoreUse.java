package com.conclearn;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * @author GregZQ
 * @create 2019-02-02 18:53
 * @des: 信号量Semaphore，用于控制同时某个资源池
 * 的连接数量。
 *  基于Semaphore实现有界容器。
 *  如果容器满了再添加的时候将会阻塞
 */
class BoundSet<T>{
    private Semaphore semaphore;
    private Set<T> set;

    public BoundSet(Integer count){
        semaphore = new Semaphore(count);
        set = new HashSet<>();
    }

    public void add(T e) throws InterruptedException {
        semaphore.acquire();

        boolean flag = false;
        try{
           flag =  set.add(e);
        }finally {
            if (!flag) {
                semaphore.release();
            }
        }
    }

    public void remove (T t){
        boolean flag = set.remove(t);
        if (flag){
            semaphore.release();
        }
    }
}
public class SemaphoreUse {

    public static void main(String args[]) throws InterruptedException {
        BoundSet<Integer> boundSet = new BoundSet<>(3);

        for (int i=0;i<6;i++){
            boundSet.add(i);
        }
    }
}
