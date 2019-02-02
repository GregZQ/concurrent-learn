package com.conclearn;

/**
 * @author GregZQ
 * @create 2019-02-02 14:14
 * @des: 指令重排序
 * 当运行多次程序的时候，输出的值可能不是40，也可能是0。
 * 这时候就是指令重排序。 赋值的顺序与机器指令当中的
 * 可能不一样。对于子线程来说可能先看到flag 为 true。
 * 然后直接数据num，此时num还没有赋值。
 */
public class ReArrangement {
    private static boolean flag;
    private static int num;
    public static void main(String args[]){
        new Thread(new Runnable() {
            public void run() {
                while (!flag){
                    Thread.yield();
                }
                System.out.println(num);
            }
        }).start();
        num = 40;
        flag =true;
    }

}
