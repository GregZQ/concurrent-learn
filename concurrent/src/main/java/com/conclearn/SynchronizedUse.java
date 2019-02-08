package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-08 10:26
 * @des: synchronized用途
 */
class Synchronizeds {

    private Object lock = new Object();

    public synchronized void show1() throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("OK");
    }

    public synchronized void show2() throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("OK");
    }

    public void show3(){
        synchronized (lock){
            System.out.println("OK");
        }
    }

    public  static synchronized void  show4() {
        System.out.println("OK");
    }

}
public class SynchronizedUse {

    public static void main(String args[]){

    }
}
