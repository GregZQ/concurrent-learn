package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-07 21:00
 * @des:
 */
public class HookUse  {

    public static void main(String args[]){
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("欢迎使用");
            }
        }));
    }
}
