package com.conclearn;

import java.util.Objects;

/**
 * @author GregZQ
 * @create 2019-02-08 14:21
 * @des: 一些volatile的使用场景
 */
//单例模式
class Singleton{

    /**
     * 如果这个地方不用volatile可能就会发生指令
     * 重排序问题。
     */
    public static volatile Singleton s;

    private Singleton(){}

    public static Singleton getInstance(){
        if (Objects.isNull(s)){
            synchronized (s){
                if (Objects.isNull(s))
                    s = new Singleton();
            }
        }
        return s;
    }
}
public class VolatileUse {
}
