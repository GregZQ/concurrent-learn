一、概览     
   
  ReentrantLock当中有三个内部类：   
  Sync、NonfairSync、FairSync。  其中NonfairSync与FairSync继承自Sync。 Sync继承自AQS。  从内部类类名就可以看出，两个子类分别实现非公平锁的获取与公平锁的获取。   
  
  **至于如何判断一个线程是否获取到锁：**  
    
  由于Lock当中的Syn继承自AQS，通过AQS维护的state，每个线程来的时候判断如果state>0，则表示当前锁已经被持有。如果未获得锁则将其放入AQS维护的同步队列当中去。  
  对于获取锁的线程来说，每次重入锁，state+1，每次从一个锁当中退出，state减一，当state=0的时候，表示当前锁可以被其他线程重新竞争。  
  **至于公平锁与非公平锁在源码上的区别**    
   非公平锁在获取锁的时候先尝试获取，获取失败的话才会放入到同步队列当中等待  
   对于公平锁在获取锁的时候先判断一下同步队列当中是否有锁以及自己自身是不是锁的持有者，如果队列为空或者自己是锁的持有者的时候才尝试获取锁。如果条件不满足则直接放入阻塞队列当中等待前面线程获取锁之后自己再进行获取。   
  
**Sync内容**  
```java  
  abstract void lock();  获取锁的方法  
  final boolean nonfairTryAcquire(int acquires) :  尝试通过非公平的方式获取锁 
  protected final boolean tryRelease(int releases) ：释放资源  
  protected final boolean isHeldExclusively() ：判断当前线程是不是锁的持有线程  
  final ConditionObject newCondition() //获取条件队列，进行await、notify操作 
  final Thread getOwner() :获取当前锁对应的线程  
  final int getHoldCount() ：获取当前获取锁的线程重入锁的次数
  final boolean isLocked()： 判断是否被锁住  
  
  //重要的方法：   
    //非安全的方式获取锁
    final boolean nonfairTryAcquire(int acquires) {
        //获取当前线程  
        final Thread current = Thread.currentThread();
        //获取当前state状态值，如果state=0表示当前锁没有被占有，可以获取
        int c = getState();
        //通过cas方式获取锁，获取成功返回true，并通过setExclusiveOwnerThread设定当前排他锁的持有者为当前线程，返回true
        if (c == 0) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        //如果state不为0，表示当前锁已经被其他线程占有，通过getExclusiveOwnerThread判断占有锁的线程是不是当前线程，如果是的话
        //将state值增加，并返回true
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        //如果到这一步，可能有两种情况  
        //1. 当前线程通过cas竞争失败  
        //2. state>0,而且锁对应的线程不是当前线程 
        //这两种情况都说明当前线程获取锁失败，返回false
        return false;
    }  
```  
**NonfairSync内容**  
```java  
   //NonfairSync继承自Sync  
   
   //当线程调用lock，方法。  
   // 1. 先通过cas操作判断如果state为0（也就是锁未被其他线程持有），将state设置为1.如果成功通过setExclusiveOwnerThread将锁对应的线程设置为当前线程，表示成功获取到锁  
   // 2. 如果上一步失败，调用acquire,如果能成功获取到资源则表示获取锁成功，如果失败通过AQS实现好的方式将锁对应的线程封装为一个Node节点放入到同步队列当中等待。
    final void lock() {
        if (compareAndSetState(0, 1))
            setExclusiveOwnerThread(Thread.currentThread());
        else
            //acquire为AQS实现好的源码，不明白的可以看之前AQS源码的内容
            acquire(1);
    }   
    
    //acquire内部调用tryAcquire尝试进行锁的获取，如果获取成功返回true，获取失败返回false
    protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
    }  
```
**FairSync内容**  
```java
    //公平方式获取锁   
    
    //通过这个方法可以发现，公平锁没有compareAndSetState这一步，因为公平的情况下需要前面阻塞的线程获取锁之后当前线程才会获取。
    //
    final void lock() {
        acquire(1);
    }
    
    //acquire内部会调用这个方法，它先通过hasQueuedPredecessors判断一下是否AQS维护的队列当中有等待获取
    //资源的值，如果有并且当前锁对应的线程不是当前线程的情况下不能获取锁。 
    //因为公平情况下处于阻塞队列当中的元素需要顺序获取锁
    
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            //如果队列为空并且获取state成功的话，将当前锁对应的线程设置为当前线程
            //表示获取资源成功
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        //如果当前锁对应的线程是当前线程的话直接将state自增
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
}
```   

***ReentrantLock其他核心方法***  
```java
    //通过构造函数可以发现默认为非公平锁
    public ReentrantLock() {
        sync = new NonfairSync();
    }
    //通过设置fair可以指定是创建公平锁还是非公平锁
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
    //释放锁
    //所谓释放锁本质上是释放锁对应的资源状态state.
    public void unlock() {
          sync.release(1);
    }
    //unlock内部调用release。而release内部调用tryRelease  
    //通过总的资源数量减去释放的资源数量判断： 
    //如果减去之后state为0，表示当前锁对应的线程可以释放锁,将锁释放,并通过AQS当中的方法通知同步队列里面的其他线程唤醒并重新获取资源
    //如果不为0，表示当前锁对应的线程还是持有者锁，所以返回false。
    protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            //判断释放锁资源的线程是不是获取锁的线程
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
    }  
    //尝试获取锁，不会阻塞。成功获取返回true。否则为false
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }
    //尝试在指定时间内获取锁，获取成功返回true。否则返回false
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
    //可相应中断的锁，如果被中断抛出InterruptException
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }
    //普通的锁
    public void lock() {
        sync.lock();
    }
```