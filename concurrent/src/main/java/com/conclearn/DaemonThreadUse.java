package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-07 16:27
 * @des: 守护线程应用
 */

public class DaemonThreadUse {

    public static void main(String args[]){
        new Thread(new Runnable() {
            @Override
            public void run() {
                 Thread a  =   new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true){
                            try {
                                System.out.println("当前存活");
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {}
                        }
                    }
                });
                 a.setDaemon(true);
                 a.start();
                try {

                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
            }
        }).start();
    }
}
