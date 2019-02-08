
##### 一、线程生命运行状态   
  （1）线程运行状态有六种：  
  
     新建（NEW）：新创建了一个线程对象，但还没有调用start()方法。  
     运行（RUNNABLE）：Java线程中将就绪（ready）和运行中（running）两种状态笼统的称为“运行”。    
     阻塞（BLOCKED）：  表示线程阻塞于锁。  
     等待（WAITING）:  进入该状态的线程需要等待其他线程做出一些特定动作（通知或中断）。  
     超时等待(TIMED_WAITING):  该状态不同于WAITING，它可以在指定的时间后自行返回。   
     终止(TERMINATED)：表示该线程已经执行完毕。   

**关于运行状态的说明： java线程将就绪与运行中状态统称为运行状态**

就绪状态：  
- 就绪状态只是说你资格运行，调度程序没有挑选到你，你就永远是就绪状态。  
- 调用线程的start()方法，此线程进入就绪状态。  
- 当前线程sleep()方法结束，其他线程join()结束，等待用户输入完毕，某个线程拿到对象锁，这些线程也将进入就绪状态。  
- 当前线程时间片用完了，调用当前线程的yield()方法，当前线程进入就绪状态。  
- 锁池里的线程拿到对象锁后，进入就绪状态。   

运行状态：     
  线程调度程序从可运行池中选择一个线程作为当前线程时线程所处的状态。  
  （2）几个状态之间的切换   
    ![image](https://note.youdao.com/yws/public/resource/4ce6ab8eb347e33e2d86d5a25c33ebe7/xmlnote/27FA3F202AE0455E8829350F2D95BFC4/5029)  
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
    原因：调用对象的wait/notify之前，必须先获取该对象的monitor

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
##### 十三、线程组   
  （1）线程组的作用：  方便线程的管理与维护。  所有线程创建的时候如何没有指定线程组，则会拿它的父线程的线程组当做自己的线程组。例如：在main线程当中创建的线程，其线程组为main线程组。线程组是一个树形结构，线程组下可以有线程，也可以有线程组。   
```java
        //main线程在启动的时候会创建一个main线程组
        System.out.println(Thread.currentThread().getThreadGroup().getName());

        //创建线程不指定线程组默认与父线程相同的线程组，这里是main线程
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public  void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        System.out.println(thread1.getThreadGroup().getName());

        //创建线程组 ,线程组创建后会自动归属到父线程所在线程组当中
        ThreadGroup threadGroup = new ThreadGroup("新的线程组");
        //获取当前线程线程组的子线程组数量
        ThreadGroup [] tg= new ThreadGroup[Thread.currentThread().getThreadGroup().activeGroupCount()];
        //将子线程组放入到tg当中
        Thread.currentThread().getThreadGroup().enumerate(tg);
        for (ThreadGroup t: tg) {
            System.out.println(t.getName());
        }

        //使用线程组的方法管理子线程
        Thread a = new Thread(threadGroup, new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("我在睡眠");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread b = new Thread(threadGroup, new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("我在睡眠");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
            }
        });

        a.start();
        b.start();
        //获取活跃的线程数
        System.out.println("活跃线程数"+threadGroup.activeCount());
        Thread.sleep(1000);
        //中断线程组当中的子线程
        threadGroup.interrupt();
```  
##### 十四、synchronized的使用以及原理  
  `synchronized`是java解决并发最常用的一种方法。作用主要有三个
   （1）线程互斥的访问同步代码
   （2）保证共享变量的修改能够及时可见  
   （3）有效解决重排序问题   
  `synchronized`使用方法：  
  
（1）修饰普通方法，此时持有锁的为当前对象     
```java
    public synchronized void show1() throws InterruptedException {
        Thread.sleep(2000);
        System.out.println("OK");
    }
```
（2）修改静态方法，此时持有的锁为整个类    
```
    public  static synchronized void  show4() {
        System.out.println("OK");
    }
```
（3）修饰代码块，此时持有的锁为代码块当中执行的对象  
```
private Object lock = new Object();
    public void show3(){
        synchronized (lock){
            System.out.println("OK");
        }
    }
```  
##### 十五、synchronized原理  

通过对代码块加锁的代码反编译，结果如下：  
![image](https://note.youdao.com/yws/public/resource/4ce6ab8eb347e33e2d86d5a25c33ebe7/xmlnote/AC56A56E99F04CB2A3EF5F38E1E89F00/4986)   
可以发现`monitorenter`,`monitorexit`两条指令。相应解释如下：  

`monitorenter` 

    每个对象有一个监视器锁（monitor）。当monitor被占用时就会处于锁定状态，线程执行monitorenter指令时尝试获取monitor的所有权，过程如下：

    1、如果monitor的进入数为0，则该线程进入monitor，然后将进入数设置为1，该线程即为monitor的所有者。

    2、如果线程已经占有该monitor，只是重新进入，则进入monitor的进入数加1.

    3.如果其他线程已经占用了monitor，则该线程进入阻塞状态，直到monitor的进入数为0，再重新尝试获取monitor的所有权。  

`montitorexit` 

    执行monitorexit的线程必须是对应的monitor的所有者。
    指令执行时，monitor的进入数减1，如果减1后进入数为0，那线程退出monitor，不再是这个monitor的所有者。其他被这个monitor阻塞的线程可以尝试去获取这个 monitor 的所有权。 

对静态方法与普通方法反编译内容如下：  

![image](https://note.youdao.com/yws/public/resource/4ce6ab8eb347e33e2d86d5a25c33ebe7/xmlnote/1847C75D04D24B129806538E6085096B/4988)

    没有发现`monitor`相关指令，但是多了ACC_SYNCHRONIZED标识符。
    调用指令将会检查方法的ACC_SYNCHRONIZED访问标志是否被设置，如果设置了，执行线程将先获取monitor，获取成功之后才能执行方法体，方法执行完后再释放monitor。
    在方法执行期间，其他任何线程都无法再获得同一个monitor对象。 其实本质上没有区别，只是方法的同步是一种隐式的方式来实现，无需通过字节码来完成。   

所以，对于synchronized锁的竞争其实就是对对应的monitor对象竞争。java每一个对象都有一个monitor对象存在于对象头中，所以每一个对象都可以作为锁。  

##### 十六、synchronized性能的提升   

  从jdk1.6开始，对于锁的性能提升做了很大的努力。 过去都是重量级锁（monitor，由用户态切换到内核态，效率较低）， 引入了：锁粗化，锁消除，适应性自旋。  同时增加了两种锁的状态：偏向锁，轻量锁。    

锁的状态有四种： 无锁，偏向锁，轻量锁，重量锁(由用户态切换到内核态，效能较低)。锁会从偏量锁升级为轻量锁，最后wi重量锁，**锁的升级是单向的**。   


synchronized用到的锁，都是存储在**对象头**中。  在HotSpot虚拟机中，对象头包括两部分信息：  
Mard Word（对象头）和Klass Pointer（类型指针）。    
   
- 类型指针，是对象指向它的类元素的指针，虚拟机通过这个指针来确定这个对象是哪个类的实例。
- 对象头又分为两部分：第一部分存储对象自身的运行时数据，例如哈希码，GC分代年龄，线程持有的锁，偏向时间戳等。这一部分的长度是不固定的。第二部分是末尾两位，存储锁标志位，表示当前锁的级别。

对象头一般占用两个机器码（32位JVM中，一个机器码等于4个字节，也就是32bit），但如果对象是数组类型，则需要三个机器码（多出的一块记录数组长度）。  
下图为对象头的变化状态：  
锁标志位 和 是否偏向锁 确定唯一的锁状态  

![image](https://note.youdao.com/yws/public/resource/4ce6ab8eb347e33e2d86d5a25c33ebe7/xmlnote/55CAC467708744CDB156BE29086E6903/4987)

`偏向锁`：  
  目的：为了在无多线程竞争的情况下，尽量减少不必要的轻量锁执行路径。
  
    在大部分情况下，锁并不存在多线程竞争，而且总是由一个线程多次获得锁。因此为了减少同一线程获取锁（会涉及到一些耗时的CAS操作）的代价而引入。  
    如果一个线程获取到了锁，那么该锁就进入偏向锁模式，当这个线程再次请求锁时无需做任何同步操作，直接获取到锁。这样就省去了大量有关锁申请的操作，提升了程序性能。

获取过程：   
  （1）检查Mark Word是否为可偏向状态，即是否为偏向锁=1，锁标志位=01  
  （2）若为可偏向状态，则检查线程ID是否为当前对象头中的线程ID，如果是，则获取锁，执行同步代码块。如果不是，进入第3步。  
  （3）如果线程ID不是当前线程ID，则通过CAS操作竞争锁，如果竞争成功。则将MarkWord中的线程ID替换为当前线程ID，获取锁，执行同步代码块。如果没成功，进入第四步。  
  （4）通过CAS竞争失败，则说明当前存在锁竞争。当执行到达全局安全点时，获得偏向锁的进程会被挂起，偏向锁膨胀为轻量级锁（重要），被阻塞在安全点的线程继续往下执行同步代码块。  
 
释放过程：  
 偏向锁的释放，采取了一种只有竞争才会释放锁的机制，线程不会主动去释放锁，需要等待其他线程来竞争。偏向锁的撤销需要等到全局安全点（这个时间点没有正在执行的代码），步骤如下：  
 （1）暂停拥有偏向锁的线程，判断对象是否还处于被锁定的状态。  
 （2）撤销偏向锁。恢复到无锁状态（01）或者 膨胀为轻量级锁。  
 
`轻量级锁`    
 轻量锁能够提升性能的依据，是基于如下假设：即在真实情况下，程序中的大部分代码一般都处于一种无锁竞争的状态（即单线程环境），而在无锁竞争下完全可以避免调用操作系统层面的操作来实现重量锁。如果打破这个依据，除了互斥的开销外，还有额外的CAS操作，因此在有线程竞争的情况下，轻量锁比重量锁更慢。  
 
获取过程：  
（1）判断当前对象是否处于无锁状态（偏向锁标记=0，无锁状态=01），如果是，则JVM会首先将当前线程的栈帧中建立一个名为锁记录（Lock Record）的空间，用于存储当前对象的Mark Word拷贝。（官方称为Displaced Mark Word）。接下来执行第2步。如果对象处于有锁状态，则执行第3步  
（2）JVM利用CAS操作，尝试将对象的Mark Word更新为指向Lock Record的指针。如果成功，则表示竞争到锁。将锁标志位变为00（表示此对象处于轻量级锁的状态），执行同步代码块。如果CAS操作失败，则执行第3步。  
（3）判断当前对象的Mark Word 是否指向当前线程的栈帧，如果是，则表示当前线程已经持有当前对象的锁，直接执行同步代码块。否则，说明该锁对象已经被其他对象抢占，此后为了不让线程阻塞，还会进入一个自旋锁的状态，如在一定的自旋周期内尝试重新获取锁，如果自旋失败，则轻量锁需要膨胀为重量锁（重点），锁标志位变为10，后面等待的线程将会进入阻塞状态。

释放轻量锁的过程：  
1.取出在获取轻量级锁时，存储在栈帧中的 Displaced Mard Word 数据。

2.用CAS操作，将取出的数据替换到对象的Mark Word中，如果成功，则说明释放锁成功，如果失败，则执行第3步。

3.如果CAS操作失败，说明有其他线程在尝试获取该锁，则要在释放锁的同时唤醒被挂起的线程。  

`重量级`锁：   
重量级锁通过对象内部的监视器（Monitor）来实现，而其中monitor本质上是依赖于低层操作系统的 Mutex Lock实现。
操作系统实现线程切换，需要从用户态切换到内核态，切换成本非常高。   

`适应性自旋`：  

大多数情况下，线程持有锁的时间不会太长，将线程挂起在系统层面耗费的成本较高。
而“适应性”则表示，该自学的周期更加聪明。自旋的周期是不固定的，它是由上一次在同一个锁上的自旋时间 以及 锁拥有者的状态 共同决定。  

具体方式是：如果自旋成功了，那么下次的自旋最大次数会更多，因为JVM认为既然上次成功了，那么这一次也有很大概率会成功，那么允许等待的最大自旋时间也相应增加。反之，如果对于某一个锁，很少有自旋成功的，那么就会相应的减少下次自旋时间，或者干脆放弃自旋，直接升级为重量锁，以免浪费系统资源。  
其他的优化方式：  

**锁粗化：**  
如果存在一系列连续的 lock unlock 操作，也会导致性能的不必要消耗.
粗化锁就是将连续的同步操作连在一起，粗化为一个范围更大的锁。  

**锁消除**  
JVM在进行JIT编译时，通过对上下文的扫描，JVM检测到不可能存在共享数据的竞争，如果这些资源有锁，那么会消除这些资源的锁。这样可以节省毫无意义的锁请求时间。

##### 十七、volatile  

（1）`volatile`的使用场景  

**防止重排序**  
```java
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
```
首先，对于实例化一个对象，可以分为下面三个步骤：  
（1）分配内存空间  
（2）初始化对象  
（3）将内存空间的地址赋值给对应的引用  
在不用volatile情况下，由于操作系统对于指令重排序，可能会变成如下情况：  
（1）分配内存空间 
（2）将内存空间的地址赋值给对应的引用 
（3）初始化对象  
如果是这样，那么多线程环境下会暴露出来一个未初始化的对象，从而导致不可预料的结果。  

**实现可见性**  
  在不加volatile的情况下，如果一个线程修改了共享变量值，但是另外一个线程看不到（每个线程都在自己的工作内存当中修改值），使用volatile可以避免这种情况。  

相应原理：   

`关于可见性`: 
 （1）修改volatile变量时会强制将修改后的值刷新到主内存当中  
 （2）修改volatile变量后会导致其他线程工作内存中对应的变量值失效，因此再读取该值时就需要重新读取主内存中的值。  
`关于有序性`：  
 java规定了一个happen-before规则：  
 
- 同一个线程中的，前面的操作 happen-before 后续的操作。（即单线程内按代码顺序执行。但是，在不影响在单线程环境执行结果的前提下，编译器和处理器可以进行重排序，这是合法的。换句话说，这一是规则无法保证编译重排和指令重排）。  
- 监视器上的解锁操作 happen-before 其后续的加锁操作。（Synchronized 规则）  
- **对volatile变量的写操作 happen-before 后续的读操作。（volatile 规则）  **
- 线程的start() 方法 happen-before 该线程所有的后续操作。（线程启动规则）  
- 线程所有的操作 happen-before 其他线程在该线程上调用 join 返回成功后的操作。  
- 如果 a happen-before b，b happen-before c，则a happen-before c（传递性）。     

而JVM对volatile变量的指令重排序规则做了限制，从而保证有序性与可见性。 ->>>>内存屏障  

##### 十八、线程池  

为什么引用`Executor线程池`:  

每次手动创建线程耗费性能   
不利于扩展，比如如定时执行、定期执行、线程中断。

Executor框架内部成员：  
1.任务：  
包括被执行任务需要实现的接口：runnable接口和callable接口 
2.任务的执行  
包括任务执行机制的核心接口Executor，以及继承自Executor的ExecutorService接口  
3.异步计算的结果  
包括future和实现future接口的futuretask类   

Executor框架最核心----线程池(`ThreadPoolExecutor`)：    

线程池的构造参数：    
```java
public ThreadPoolExecutor(int corePoolSize,int maximumPoolSize,long keepAliveTime,TimeUnit unit,
        BlockingQueue<Runnable> workQueue,ThreadFactory threadFactory,RejectedExecutionHandler handler);
```  
各构造参数的含义：  
- corePoolSize：核心池的大小，在创建了线程池后，默认情况下，线程池中并没有任何线程，而是等待有任务到来才创建线程去执行任务，除非调用了prestartAllCoreThreads()或者prestartCoreThread()方法，从这2个方法的名字就可以看出，是预创建线程的意思，即在没有任务到来之前就创建corePoolSize个线程或者一个线程。默认情况下，在创建了线程池后，线程池中的线程数为0，当有任务来之后，就会创建一个线程去执行任务，当线程池中的线程数目达到corePoolSize后，就会把到达的任务放到缓存队列当中；  
- maximumPoolSize：线程池最大线程数，这个参数也是一个非常重要的参数，它表示在线程池中最多能创建多少个线程；  
- keepAliveTime：  表示线程没有任务执行时最多保持多久时间会终止。默认情况下，只有当线程池中的线程数大于corePoolSize时，keepAliveTime才会起作用，直到线程池中的线程数不大于corePoolSize，即当线程池中的线程数大于corePoolSize时，如果一个线程空闲的时间达到keepAliveTime，则会终止，直到线程池中的线程数不超过corePoolSize。但是如果调用了allowCoreThreadTimeOut(boolean)方法，在线程池中的线程数不大于corePoolSize时，keepAliveTime参数也会起作用，直到线程池中的线程数为0；  
- unit：参数keepAliveTime的时间单位  
- workQueue：一个阻塞队列，用来存储等待执行的任务，这个参数的选择也很重要，会对线程池的运行过程产生重大影响，一般来说，这里的阻塞队列有以下几种选择：  
```java
ArrayBlockingQueue;
LinkedBlockingQueue;
SynchronousQueue;
```  
- threadFactory：线程工厂，主要用来创建线程； 
- handler：表示当拒绝处理任务时的策略，有以下四种取值：  
```java
ThreadPoolExecutor.AbortPolicy:丢弃任务并抛出RejectedExecutionException异常。 
ThreadPoolExecutor.DiscardPolicy：也是丢弃任务，但是不抛出异常。 
ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
ThreadPoolExecutor.CallerRunsPolicy：由调用线程处理该任务 
```    
**几个参数的协作关系**  
l 如果线程池中的数量小于corePoolSize，即使线程池中的线程都处于空闲状态，也要创建新的线程来处理被添加的任务。

2 如果线程池中的数量等于corePoolSize，但是缓冲队列workQueue未满，那么任务被放入缓冲队列。

3如果线程池中的数量大于corePoolSize，缓冲队列workQueue满，并且线程池中的数量小于maximumPoolSize，建新的线程来处理被添加的任务。

4.如果线程池中的数量大于corePoolSize，缓冲队列workQueue满，并且线程池中的数量等于maximumPoolSize，那么通过handler所指定的策略来处理此任务。也就是：处理任务的优先级为：核心线程corePoolSize、任务队列workQueue、最大线程maximumPoolSize，如果三者都满了，使用handler处理被拒绝的任务。

**内部实现的几种线程池**

`FixedThreadPool`:  
  创建一个固定数量线程池  
```java
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
    }
```    
- FixedThreadPool的corePoolSize和maxiumPoolSize都被设置为创建FixedThreadPool时指定的参数nThreads。  
- 0L则表示当线程池中的线程数量操作核心线程的数量时，多余的线程将被立即停止  
- 最后一个参数表示FixedThreadPool使用了无界队列LinkedBlockingQueue作为线程池的做工队列，由于是无界的，当线程池的线程数达到corePoolSize后，新任务将在无界队列中等待，因此线程池的线程数量不会超过corePoolSize，同时maxiumPoolSize也就变成了一个无效的参数，并且运行中的线程池并不会拒绝任务。  

任务执行：   
1.如果当前工作中的线程数量少于corePool的数量，就创建新的线程来执行任务。

2.当线程池的工作中的线程数量达到了corePool，则将任务加入LinkedBlockingQueue。

3.线程执行完1中的任务后会从队列中去任务。

注意LinkedBlockingQueue是无界队列，所以可以一直添加新任务到线程池。  
`SingleThreadExecutor`:  
使用单个工作线程执行任务  
```java
public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService
            (new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>()));
}
```    
任务执行：  
执行过程如下：

1.如果当前工作中的线程数量少于corePool的数量，就创建一个新的线程来执行任务。

2.当线程池的工作中的线程数量达到了corePool，则将任务加入LinkedBlockingQueue。

3.线程执行完1中的任务后会从队列中去任务。

注意：由于在线程池中只有一个工作线程，所以任务可以按照添加顺序执行。  

`CachedThreadPool`:  
是一个”无限“容量的线程池，它会根据需要创建新线程
```java  
public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>());
}
```   
CachedThreadPool的corePoolSize被设置为0，即corePool为空；maximumPoolSize被设置为Integer.MAX_VALUE，即maximum是无界的。这里keepAliveTime设置为60秒，意味着空闲的线程最多可以等待任务60秒，否则将被回收。
 
CachedThreadPool使用没有容量的SynchronousQueue作为主线程池的工作队列，它是一个没有容量的阻塞队列。每个插入操作必须等待另一个线程的对应移除操作。这意味着，如果主线程提交任务的速度高于线程池中处理任务的速度时，CachedThreadPool会不断创建新线程。极端情况下，CachedThreadPool会因为创建过多线程而耗尽CPU资源  

任务执行：  
1.首先执行SynchronousQueue.offer(Runnable task)。如果在当前的线程池中有空闲的线程正在执行SynchronousQueue.poll()，那么主线程执行的offer操作与空闲线程执行的poll操作配对成功，主线程把任务交给空闲线程执行。，execute()方法执行成功，否则执行步骤2

2.当线程池为空(初始maximumPool为空)或没有空闲线程时，配对失败，将没有线程执行SynchronousQueue.poll操作。这种情况下，线程池会创建一个新的线程执行任务。

3.在创建完新的线程以后，将会执行poll操作。当步骤2的线程执行完成后，将等待60秒，如果此时主线程提交了一个新任务，那么这个空闲线程将执行新任务，否则被回收。因此长时间不提交任务的CachedThreadPool不会占用系统资源。

SynchronousQueue是一个不存储元素阻塞队列，每次要进行offer操作时必须等待poll操作，否则不能继续添加元素。  


十九、J.U.C包当中的工具  

 1. 同步工具类：    
`CountDownLatch`:  
作用：确保某些活动在其他活动完成后才继续运行  
```java
    //计算某个并发任务执行的时间
    private static CountDownLatch start  = new CountDownLatch(1);
    private static CountDownLatch end    = new CountDownLatch(10);

    public static void main(String args[]) throws InterruptedException {
        for (int i=0;i<10;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await();
                        Thread.sleep(1000);
                        end.countDown();
                    } catch (InterruptedException e) {} finally {

                    }
                }
            }).start();
        }
        long startTime = System.currentTimeMillis();
        start.countDown();
        end.await();
        long endTime   = System.currentTimeMillis();
        System.out.println(endTime-startTime);
    }
```    
`futuretask`:
作用：可以阻塞等待运算结果完成。  
```java
FutureTask<Integer> futureTask = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(5000);

                return 1000;
            }
        });

        Thread thread = new Thread(futureTask);
        //线程启动
        thread.start();

        //阻塞获取结果，达到同步性
        Integer result = futureTask.get();
        System.out.println(result);
```  
`Semaphore`  
作用：用来控制同时访问某个特定资源的操作数量，或者同时执行某个指定操作的数量。  
```java
//模拟有界队列
class BoundSet<T>{
    private Semaphore semaphore;
    private Set<T> set;

    public BoundSet(Integer count){
        semaphore = new Semaphore(count);
        set = new HashSet<>();
    }

    public void add(T e) throws InterruptedException {
        semaphore.acquire();

        boolean flag = false;
        try{
           flag =  set.add(e);
        }finally {
            if (!flag) {
                semaphore.release();
            }
        }
    }

    public void remove (T t){
        boolean flag = set.remove(t);
        if (flag){
            semaphore.release();
        }
    }
}
public class SemaphoreUse {

    public static void main(String args[]) throws InterruptedException {
        BoundSet<Integer> boundSet = new BoundSet<>(3);

        for (int i=0;i<6;i++){
            boundSet.add(i);
        }
    }
}   
```  
`Cyclicbarrier`:  
栅栏。表示n个线程互相等待，任何一个线程做完之后都需要等待其他线程。  
```java
//模拟跑步，准备完毕后同时起跑
class Runner implements Runnable{
    private String name ;
    private CyclicBarrier cyclicBarrier;

    public Runner(String name, CyclicBarrier barrier){
        this.name = name;
        this.cyclicBarrier = barrier;
    }

    @Override
    public void run() {
        System.out.println(name +" 已经准备 ");
        try {
            cyclicBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        System.out.println(name+ " go! ");
    }
}
public class CyclicBarrierUse {

    private static CyclicBarrier cyclicBarrier= new CyclicBarrier(5);

    public static void main(String args[]){
        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);
        for (int i =0 ;i<10;i++){
            new Thread(new Runner(i+"",cyclicBarrier)).start();
        }

    }
}
```  
CyclicBarrier与CountDownLatch的区别：  
语义上：    
CountDownLatch是一个同步的辅助类，允许一个或多个线程，**等待其他一组线程完成操作**，再继续执行。  
CyclicBarrier是一个同步的辅助类，允许一组线程相互之间等待，达到一个共同点，再继续执行。     

而且CyclicBarrier在释放等待线程后可以重用，所以称它为循环（Cyclic）的屏障（Barrier）。  
`Exchange`：  
作用：用于偶数个线程之间使用同一个管道交换数据。    
2.其他并发工具：   
 
`ConcurrentHashMap`:  
在并发方面的优点：

    使用分段锁来实现更大程度的共享。 执行读取操作的线程和执行更改操作的线程可以并发问map，并且一定数量的写入线程可以并发修改map。
    使用弱一致性的迭代器来容忍并发的修改，当创建迭代器时会遍历已有的元素，并可以将修改操作反映给容器。   
    将size和isempty语义减弱，返回一个估计值（因为并发条件下这两个值的用处也不会很大）
    没有实现对map加锁以提供独占访问。  
    
`CopyOnWriteArrayList`:    
    
    在修改容器的时候，它会生成一份当前容器的副本。迭代器指向一个基础数组的引用从而实现读写分开。 
    但仅当读操作多余写操作的时候才会使用它。适用于读多写少的情况。  

`阻塞队列`：

    阻塞队列有
    ArrayListBlockQueue：底层是数组的阻塞队列
    LinkListBlockQueue： 底层是链表的阻塞队列
    PriorityBlockingQueue：阻塞队列当中插入的数据按照优先级关系取出。
    SynchronousQueue：只能放一个元素的阻塞队列，放入的数据必须等待另外的线程取出。否则其他线程将无法放入数据。    
    
    
接下来会进行J.U.C包比较重要的类的源码分析。。。。。。。。

  
    

    
   
  
