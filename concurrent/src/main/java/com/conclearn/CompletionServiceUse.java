package com.conclearn;

import java.util.concurrent.*;

/**
 * @author GregZQ
 * @create 2019-02-03 11:19
 * @des:  通过它可以实现多个计算任务在执行的时候。
 * 获取结果不会顺序阻塞。当一个任务执行完成后会立即从计算队列当中
 * 取出并执行。从而提高效率（CompletionService）是Executor与BlockQueue的
 * 整合。
 */
class MyLongTimeSum implements Callable<Integer>{
    //表示一个长时间计算
    private Integer num;
    public MyLongTimeSum(Integer num){
        this.num = num;
    }
    @Override
    public Integer call() throws Exception {
        Thread.sleep(3000);
        return num;
    }
}
public class CompletionServiceUse {

    public static void main(String args[]) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(8);

        CompletionService<Integer> completionService =
                new ExecutorCompletionService<Integer>(executorService);

        for (int i=0;i<10;i++){
            completionService.submit(new MyLongTimeSum(i));
        }

        for (int i=0;i<10;i++){
           Future<Integer> future =  completionService.take();
            System.out.println(future.get());
        }
    }
}
