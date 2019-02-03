package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-03 17:53
 * @des: 模拟线程死锁
 */
class DealLock implements Runnable{
    private Integer from;
    private Integer to;

    public DealLock(Integer from,Integer to){
        this.from =from;
        this.to=to;
    }

    @Override
    public void run() {
        synchronized (from) {
            System.out.println("我获得了from，准备去获得to");
            synchronized (to) {
                System.out.println("我获得了to，准备去获得from");
            }
        }
    }
}
public class DealLockUse {
    public static void main(String args[]){
        Integer va1 = new Integer(10);
        Integer va2 = new Integer(20);
        DealLock a = new DealLock(va1,va2);
        DealLock b = new DealLock(va2,va1);

        new Thread(a).start();
        new Thread(b).start();
    }
}
