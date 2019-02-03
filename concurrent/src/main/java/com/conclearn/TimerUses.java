package com.conclearn;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author GregZQ
 * @create 2019-02-03 10:20
 * @des:  关于Timer。
 *   Timer可以周期或者指定时间执行一个任务
 *   Timer的缺陷：Timer只能用一个线程去执行所有定时任务，
 *   可能会造成定时任务执行的不准确。而且如果抛出异常，Timer
 *   也不会捕获，剩下的任务也不会执行直接退出。
 */
class TimeTask extends TimerTask {
    @Override
    public void run() {
        System.out.println("OK");
    }
}
public class TimerUses {

    public static void main(String args[]){
        Timer timer = new Timer();
        timer.schedule(new TimeTask(),1000,3000);
    }
}
