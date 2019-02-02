package com.conclearn;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author GregZQ
 * @create 2019-02-02 20:08
 * @des: 实现一个线程安全的缓存
 */
//表示一个长时间的计算
interface LongTimeSum<T,V>{
    V compute(T t);
}
class MyLongTimsSum implements LongTimeSum<Integer,BigInteger>{

    @Override
    public BigInteger compute(Integer integer) {
        int i =integer;
        return new BigInteger(String.valueOf(i));
    }
}
class Cache<T,V>{
    ConcurrentMap<T,Future<V>> map = new ConcurrentHashMap<>();
    LongTimeSum<T,V> longTimeSum ;

    public Cache(LongTimeSum<T,V> longTimeSum){
        this.longTimeSum= longTimeSum;
    }

    public V get(T a){
        while(true){
            Future<V> future = map.get(a);
            if (Objects.isNull(future)){
                Callable<V> call = new Callable() {
                    @Override
                    public V call() throws Exception {
                        return longTimeSum.compute(a);
                    }
                };
                FutureTask<V> futureTask = new FutureTask<V>(call);
                future =  map.putIfAbsent(a,futureTask);
                if (future == null){
                    future = futureTask;
                    futureTask.run();
                }

                try {
                    return future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
public class CacheUtil {

    public static void main(String args[]){

        MyLongTimsSum myLongTimsSum = new MyLongTimsSum();

        Cache<Integer,BigInteger> cache = new Cache<>(myLongTimsSum);
    }
}
