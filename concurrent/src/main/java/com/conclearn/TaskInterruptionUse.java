package com.conclearn;

import java.util.concurrent.*;

/**
 * @author GregZQ
 * @create 2019-02-07 19:31
 * @des: 任务中断策略，如何优雅的关闭一个线程 。
 */
//通过使用标志的形式取消任务
class Runnable1 implements Runnable{
    private  volatile  boolean flag = true;
    @Override
    public void run() {
            int count =0;
            for (int i =0 ; i < 1000000000; i++) {
                count +=1;
                if (!flag){
                    break;
                }
            }
            if (flag){
                System.out.println(count);
            }
    }
    public void cancel(){
        this.flag =false;
    }
}
//通过简单的中断策略，如果是中断的话，抛出中断异常。
//否则自己打印消息
class  Runnable2 implements Runnable{
    @Override
    public void run() {
        try {
            int count =0;
            for (int i =0;i<100000000;i++) {
                //执行一个耗时操作
                count+=1;
                if (Thread.currentThread().isInterrupted()){
                    break;
                }
            }
            if (Thread.interrupted()){
                //清除中断标志，抛出异常
                throw new InterruptedException();
            }else{
                System.out.println(count);
            }
        }catch (InterruptedException e){
            System.out.println("任务已经被中断");
        }
    }
}
public class TaskInterruptionUse {

    public static void main(String args[]) throws InterruptedException, TimeoutException, ExecutionException {
        Runnable1 runnable1 = new Runnable1();

        new Thread(runnable1).start();

        Thread.sleep(100);

        runnable1.cancel();

        Runnable2 runnable2 =new Runnable2();
        Thread thread2 =new Thread(runnable2);
        thread2.start();
        Thread.sleep(100);
        thread2.interrupt();

    }
}
