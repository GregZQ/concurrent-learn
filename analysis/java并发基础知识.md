##### 一、线程生命运行状态   
  （1）线程运行状态有五种：  新建、就绪、执行、阻塞、终止  
  （2）几个运行状态之间的切换   
    ![image](http://assets.processon.com/chart_image/5c4fb627e4b0f0908a8ae1b2.png)  
##### 二、线程创建的几种方式   
   （1） 使用Thread类，重写run方法后start   
```
class  MyThread1 extends Thread{
    @Override
    public void run() {
        System.out.println("继承thread");
    }
}

//运行代码
        MyThread1 myThread1 = new MyThread1();
        myThread1.start();
```
  （2）实现Runnable接口   
```
class MyRunnable implements Runnable{
    @Override
    public void run() {
        System.out.println("实现runnable接口");
    }
}

//运行 
        MyRunnable myRunnable = new MyRunnable();
        new Thread(myRunnable).start();
```
(3)通过实现callable接口，并与futuretask配合  
```
class MyCallable implements Callable<String>{

    @Override
    public String call() throws Exception {
        System.out.println("实现callable接口");
        return "cllable";
    }
}
//运行  
        MyCallable myCallable = new MyCallable();
        FutureTask<String> futureTask = new FutureTask<String>(myCallable);
        new Thread(futureTask).start();
        System.out.println(futureTask.get());
```
（4）通过线程池运行callable或者runnable 
```
//运行
        ExecutorService executors= Executors.newFixedThreadPool(3);
        Future<String> future = executors.submit(myCallable);
        System.out.println(future.get());
        executors.execute(myRunnable); 
```
四种方式选择：  
 （1）首先实现Thread的话会被继承给局限住，而且不符合单一职能原则。所以它最不被推荐。   
 （2）实现runnable将任务与线程分开，符合单一职能原则，而且任务创建的对象如果有数据可以被多个线程共享。  
 （3）如果需要返回值的话推荐使用callable  
 （4）线程池会创建一组线程，避免了单个线程的创建与销毁。如果运行的是多个线程，推荐使用线程池的方式
##### 三、为什么会有runnbale接口,与Thread相比有什么好处  
    
（1）为了符合单一职能原则，将线程与任务分开  
（2）避免了java单继承带来的局限性  
（3）增强程序健壮性，代码可以被多个线程共享。如果多个线程操作的是相同的runnable对象，可以实现数据的共享。  

##### 四、创建线程时候进行的操作  
```
    public Thread() {
        init(null, null, "Thread-" + nextThreadNum(), 0);
    }
    private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        this.name = name;
        //获取执行这个线程的线程（父线程）
        Thread parent = currentThread();
        SecurityManager security = System.getSecurityManager();
        if (g == null) {
            if (security != null) {
                g = security.getThreadGroup();
            }
            if (g == null) {
                g = parent.getThreadGroup();
            }
        }
        g.checkAccess();
        if (security != null) {
            if (isCCLOverridden(getClass())) {
                security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
            }
        }
        //在父线程组上未开始的线程数加一
        g.addUnstarted();
        //设置线程组，是否是守护线程，线程优先级。
        this.group = g;
        this.daemon = parent.isDaemon();
        this.priority = parent.getPriority();
        if (security == null || isCCLOverridden(parent.getClass()))
            this.contextClassLoader = parent.getContextClassLoader();
        else
            this.contextClassLoader = parent.contextClassLoader;
        this.inheritedAccessControlContext =
                acc != null ? acc : AccessController.getContext();
        this.target = target;
        setPriority(priority);
        if (parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        //设置栈大小
        this.stackSize = stackSize;
        //设置一个全局自增的id
        tid = nextThreadID();
    }
```  
  当调用默认的new Thread的时候，最终会调用这个方法。   
  线程创建流程 ：  
  （1）为线程设置名字。名称为Thread-***（后面的值是静态自增的数）。
  （2）获取执行这个线程的当前线程 
  （3）如果线程组为空的话，设置线程组（如果系统安全器不为空的话与其一个组，否则与parent线程一个线程组）  
  （4）往线程组添加未启动的线程数  
  （5）设置与父线程一样是否是守护线程、优先级  （这些都可以自己修改）   
  （6）设置栈的大小  
  （7）设置线程id  （这是一个静态自增的数）   

为线程初始化的时候有一个初始化方法可以指定栈的大小
```
    public Thread(ThreadGroup group, Runnable target, String name,
                  long stackSize) {
        init(group, target, name, stackSize);
    }
```  
stackSize可以自定义调整分配给每个线程的栈大小。如果分配给某个线程的栈大小越大，那么它执行方法递归的深度也就越深。 如果分配给线程的栈越小，那么递归的深度也就越浅，但是并存的线程也越多，因为分配给每个线程的栈的空间小了，进而可以有多个线程共存。   

    注意：
       当某个线程递归调用的深度超出极限后，会抛出：java.lang.StackOverflowError
       当创建的线程没有足够的空间分配给它作为栈空间的时候，会抛出:java.lang.OutOfMemoryError      

##### 五、守护线程应用   
  
  守护线程与应用线程的区别:如果守护线程的父线程终结，那么守护线程也会退出。JVM只有在最后一个非守护线程退出的时候才会退出。   
  
```java
 //应用，模拟集群当中多个服务器的心跳检测，如果服务器挂掉了，那么守护线程也应该退出。
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
```   
##### 六、Thread.join   
   作用： 当我们调用某个线程的这个方法时，这个方法会挂起调用线程，直到被调用线程结束执行，调用线程才会继续执行。
```java
//源码解读
    public final void join() throws InterruptedException {
        join(0);
    }
    public final synchronized void join(long millis)
    throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (millis == 0) {
            while (isAlive()) {
                wait(0);
            }
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }
```  
通过源码可以发现：   
   `join（）`或调用`join(0)`这个方法，而join(0)当中通过native方法`isAlive`判断当前线程是否存在，如果存活的话就让出cpu，阻塞等待下次获得cpu的时候再检查。如果发现线程不存活了，表示执行完成。这时候会返回。  而设置版本时间的`join(long time)`同理

```应用
class WinBottleRunnable implements Runnable{

    @Override
    public void run() {
        System.out.println("我在生产酒瓶");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
        System.out.println("生产酒瓶完成");
    }
}
class LoadingWineRunnable implements Runnable{
    public Thread winBottleThread;

    public LoadingWineRunnable(Thread winBottleThread){
        this.winBottleThread = winBottleThread;
    }

    @Override
    public void run() {
        try {
            winBottleThread.join();
            System.out.println("我在装酒");
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
        System.out.println("装酒完成");
    }
}
class PackWineRunnable implements Runnable{
    Thread loadingWineThread;

    public PackWineRunnable(Thread loadingWineThread){
        this.loadingWineThread = loadingWineThread;
    }

    @Override
    public void run() {
        try {
            loadingWineThread.join();
            System.out.println("我在打包");
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        System.out.println("打包完成");
    }
}

public class ThreadJoinUse {
    public static void main(String args[]) throws InterruptedException {
        Thread a = new Thread(new WinBottleRunnable());

        Thread b = new Thread(new LoadingWineRunnable(a));

        Thread c=  new Thread(new PackWineRunnable(b));

        a.start();
        b.start();
        c.start();
        System.out.println("OK");

    }
}  
```  
##### 七、线程中断  
 
 所谓中断就是给线程打上一个中断标志。如果线程没有处理中断的逻辑那么不会相应这个中断（所谓逻辑就是是否处理中断）。  
 
    方法名|作用
    :------------------------------:|:-----------------------:
    java.lang.Thread#interrupt      | 中断目标线程，给目标线程发一个中断信号，线程被打上中断标记。  
    java.lang.Thread#isInterrupted()| 判断目标线程是否被中断，不会清除中断标记。
    java.lang.Thread#interrupted    | 判断目标线程是否被中断，会清除中断标记   

任务中断策略：    
    （1） 如果遇到的是可中断的阻塞方法抛出InterruptException，那么在得到异常之后可以继续向上抛出。  
    （2） 如果是检测到中断，则可清除中断标志，并向上抛出InterruptException  
    （3） 如果不方便抛出InterruptException，那么可以捕获中断方法抛出的InterruptException，并通过`java.lang.Thread#interrupt`重新设置中断状态。  
    对于中断，尽量不要不去处理。  

注意：   
   （1）如果是`wait`,`join`,`sleep`等处于WAITING\TIMED_WAITING状态下的线程，线程被中断首先会清除中断标志位，然后再抛出InterruptException  
   （2）处于运行状态RUNNABLE、阻塞状态BLOCKED的线程不会被打断，但是其中断标志位被设置为true  
   （3）对新建NEW、终止TERMINATED下的线程调用interrupt不会产生任何效果      

##### 八、如何优雅的取消任务  

首先需要明白，取消任务的执行，可以使用中断。 也就是说中断是取消任务执行的一种方式，而且是取消任务最合理的方式。而java内置都是通过中断取消任务。
  
  （1）通过设置某个变量的方式（自定义取消策略）   
```
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
//运行
        Runnable1 runnable1 = new Runnable1();

        new Thread(runnable1).start();

        Thread.sleep(100);

        runnable1.cancel();
```
  （2）使用java内置中断机制取消任务，但需要注意将任务必须设计成可相应中断的任务。  
```
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
//运行
        Runnable2 runnable2 =new Runnable2();
        Thread thread2 =new Thread(runnable2);
        thread2.start();
        Thread.sleep(100);
        thread2.interrupt();
```
    除了自己手动关闭之外，还可以使用内置的`future.cancale`,`executor.shoutDownNow`，但都需要将任务设计成可中断的。  
 （3）对于一些不可中断的阻塞，比如SocketIO等，这些需要知道阻塞的原因，然后自己设计中断方法进行关闭。     
 
##### 八、死锁  
 
 （1）死锁检测工具  jps/jstack jps/jconsole    
 （2）如何避免死锁  

    （1）使用定时的锁，在定时时间内无法获得资源的时候置为失败 
    （2）避免同一个线程持有多个锁    
##### 九、条件队列   
  （1）什么是条件队列：当某个条件不满足的时候，挂起自身并释放锁，一旦条件满足则醒来  
  （2）条件队列的方法：  `object.wati`,`object.notify`,`object.notifyAll`  
  （3）条件队列满足三元关系：  
     
    内置条件队列关注的谓词必须由一个锁来保护，而且锁对象与wait、notify必须是同一个对象。  

   （4）扩展的条件队列  
   Lock中的Condition，一个lock可以创建多个。（await,singal），可以实现多个线程等待的条件不同情况下通知唤醒指定的一组线程。     

##### 十、wait与sleep的区别  
    （1）`sleep`是`Thread`中的静态方法，`wait`是`Object`当中的方法  
    （2）`sleep`不会释放当前线程持有的锁，而`wait`会释放当前线程持有的锁
    （3）`sleep`和`wait`都会使线程进入`TIME-WAITING`状态释放cpu的执行权，但调用`sleep`线程在设定的实现内能够自动返回，而`wait`需要等待被唤醒并获得锁之后才能返回   

##### 十一、为应用程序注入钩子  
   
钩子就是当应用被外界终止之后执行的方法。  
当时用win下 ctrl+c 或者linux下kill pid都会执行。

```
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("欢迎使用");
            }
        }));
```  
##### 十二、如何捕获线程运行时候的异常   
   
   （1）对于runnable的线程。 可以设置异常处理Handler,为每一个线程添加一个异常处理器。`Thread.UncaughtExceptionHandler.`。里面的`uncaughtException`方法将会捕获线程无法捕获的异常
```java
//用法  
class  MyUncaughtException implements Thread.UncaughtExceptionHandler{

    //也可以将其追加到日志当中去
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.out.println("线程名称为:"+t.getName()+"，抛出异常未"+e.getMessage());
    }
}

//运行
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int i =1/0;
            }
        };

        Thread thread = new Thread(runnable);
        thread.setUncaughtExceptionHandler(new MyUncaughtException());
        thread.start();
```  
 如果是使用的Executor，可以在ThreadFactory中设置,但是需要注意，这只对`execute`有效,而submit中，产生的异常将被当做future的一部分返回。不会被异常处理器捕获。
```java
        ExecutorService executors = Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setUncaughtExceptionHandler(new MyUncaughtException());
                return thread;
            }
        });
        executors.execute(runnable);
```  
如果所有线程只需要一个线程处理器的话，也可以这样设置。  
```java
 //如果没有指定线程的异常处理器，线程组也没有，就会使用默认的
Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtException());
```
 (2)可以实现callable接口，通过future或者futuretask来捕获异常。callable可以获得线程的返回值，同样可以获得线程的异常。  
 
（3）通过重写线程池`ThreadPoolExecutor.afterExecute`方法  
```java
class MyThreadPoolExecutor extends ThreadPoolExecutor{

    public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (r instanceof Runtime){
            if (Objects.nonNull(t)){
                System.out.println(t);
            }
        }else if (r instanceof FutureTask){
            FutureTask futureTask = (FutureTask) r;
            try {
                futureTask.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                //处理捕获的异常
            }
        }
    }
}
```


    

    
   
  
