    
  AQS类似于一个框架。作为J.U.C常用并发工具的基本基类。它维护了一个state(核心)，一个列表。AQS有两套维护共享状态的方式：独占，共享。也就是说，如果你自己想写一个基于AQS的并发类，你只需要重写其中一种（独占或共享）访问资源的方式即可。另外对于两套共享资源的方式又可分为可中断的与不可中断的，两者区别就是可中断可以抛出一个中断异常，其他基本一样，所以我只阅读了可中断的。
  
## 基本源码
  
  #### 属性
```java
    
    /**
    代表需要维护的同步状态，所有线程想获取资源的时候
    都需要去竞争这个状态，获取到才可以进行接下来的操作。
    这样就明白，如果是独占，state为1。 
                      共享，state为n
    **/
    private volatile int state;
    
    /**
     所维护等待队列的头结点
    **/
    private transient volatile Node head;
    
    /**
      所维护等待队列的尾结点
    **/
    private transient volatile Node tail; 
    /**
    
     等待队列的结点都是一个个node
    **/
    static final class Node {
        //共享模式结点标志
        static final Node SHARED = new Node();
        //独占模式结点标志
        static final Node EXCLUSIVE = null;
        //表示当前结点处于无效状态
        static final int CANCELLED =  1;
        //表示等待获取线程
        static final int SIGNAL    = -1;
        //条件状态
        static final int CONDITION = -2;
        //在共享模式中使用表示获得的同步状态会被传播
        static final int PROPAGATE = -3;
        //另外还有一个0状态，不在上面几种之内
    }

```
#### 独占方式获取资源
```java
    /**
    尝试以独占方式获取资源，每个独占类必须实现这个方法，获取成功返回true，否则是false
    **/
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }
    /**
     尝试释放独占的资源，每个独占类必须实现此方法,释放成功返回true，否则是false
    **/
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }
    
    
    /**
       获取独占资源，如果尝试获取没有成功则进入等待队列
    **/
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
    
    /**
      当进入到这个方法表示尝试获取资源没有成功，这时候通过
      一次cas操作尝试快速将当前节点放入等待队列末尾。如果
      尾结点为空或者快速放入失败则进入enq方法。
    **/
    private Node addWaiter(Node mode) {
        //表示未获取资源成功的结点
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }    
    
    /**
     进入这个方法表示快速放入队列末尾失败。
     这时候通过自旋操作将当前结点放入队列末尾
    **/
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            //如果头结点为空，则通过cas操作将当前结点放在头部
            if (t == null) { 
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
            //自旋操作放入到队列末尾，并将tail指向这个结点
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }  
    /**
      当将结点放入队列后，会先尝试一次获取资源，
      如果获取不成功则被挂起等待被唤醒，并让出
      cpu资源。
    **/
    
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                /**
                获取新插入结点的前一个，
                如果前一个为头结点并且获取资源成功，
                则将当前结点设置为头结点。并返回
                中断状态。
                **/
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                /**
                  到这表示获取失败，这时候先调用
                  shouldParkAfterFailedAcquire方法，查看一下当前结点的
                  前面结点是否有效，即结点的waitStatus是否小于等于0。
                 如果大于0表示结点无效，这时候需要将链表中的无效结点去
                 除掉，并自旋尝试重新获取结点。如果之前结点都是有效的，这
                 时候会进入parkAndCheckInterrupt将当前线程挂起，等待被唤
                 醒，并返回挂起过程中是否被中断的状态。
                **/
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
    
    /**
       这个类的功能就是过滤掉队列当中无效的结点。如果之前的结点存在无
       效的，则会向前找，直到找到最后的有效结点并跟在其后，并返回false
       尝试重新获取资源。
    **/
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            return true;
        if (ws > 0) {

            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }
    /**
        当进入这个方法是会真的被挂起，这时候剩下的不会被执行直到被唤醒。
    **/
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }
```
#### 独占方式释放资源
```java
   /**
     先尝试释放资源调用tryRelease方法。这个方法需要人员自己实现，如果
     获取释放成功则调用unparkSuccessor方法，先将头结点状态改为0,    然后将之后的第一个结点唤醒，来获取资源。
   **/
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }
    /**
    
    **/
    private void unparkSuccessor(Node node) {
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }

```
#### 共享方式获取资源
```java
    /**
    共享的方式获取资源，尝试获取资源tryAcquireShared，获取失败则调用doAcquireShared，在满足条件下尝试获取一波资源，获取不到则会被挂起
    **/
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }
    /**
      这方法与独占类似，不同处就是将打断方法放在前面。
      不过我觉得功能上没什么区别，从这可以看到，如果队列
      前面获取不到共享资源则后面的会被阻塞。
    **/
    private void doAcquireShared(int arg) {
       //创建一个共享的结点放到队列的尾部
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                /**
                  如果当前结点是头结点的下一个，则尝试获取资源。
                **/
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                /**
                   流程与独占一样：
                      1.如果有无用结点，则去掉并再次尝试获取
                      2.如果没有则会进入挂起方法。
                      
                **/
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```
#### 共享方式释放资源
```java
    /**
      释放共享资源，释放后调用doReleaseShared方法。
    **/
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }
    /**
      释放掉当前结点，并调用unparkSuccessor方法唤醒后继结点
    **/
    private void doReleaseShared() {
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {//将头结点状态设置为0
            
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;
                    //唤醒后继结点
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                
            }
            if (h == head)                   
                break;
        }
    }    
```
#### 关于ConditionObject
```java
        /** 
            用于维护await后挂起的结点列表
        **/
        private transient Node firstWaiter;
        private transient Node lastWaiter;
```
##### await操作，将当前获取资源的结点挂起，可以被signal唤醒
```java

        /**
         该操作是对获取资源的结点有效,用于释放当前结点占有资源并挂起
        **/
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            /**
              根据线程创建一个等待结点,将其加入Condition维护的队列。
              这样就说明将当前线程对应的结点从获取资源的队列中移除了。
             **/
            Node node = addConditionWaiter();
            //释放当前结点占有的资源
            int savedState = fullyRelease(node);
            int interruptMode = 0;‘
            /**
              判断当前队列是否在获取资源的队列当中，如果不在，则挂起
            **/
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                /** 
                当到这一步后，表示结点被唤醒，然后调用
                checkInterruptWhileWaiting判断结点是
                如何被唤醒的，signal或发生异常。
                **/
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            /**
             到这将结点加入等待获取资源的队列当中。
             并将结点从condition队列中移除，用
             reportInterruptAfterWait反馈当前结点被唤醒的状态。
             **/
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) 
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }
        /**
          如果线程没被打断直接返回0，如果被打断判断一下是在signal
          之前还是之后。REINTERRUPT表示之后是被正常唤醒，THROW_IE表示发生异常
        **/
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                0;
        }
```
##### signal，唤醒await后的结点，与await是成对的操作
```
       //唤醒操作，会将当前维护队列的头结点放入资源获取队列中，等待重新获取资源
        public final void signal() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignal(first);
        }
        /**
        具体的唤醒操作，调用transferForSignal方法，尝试去唤醒头结
        点，唤醒成功则退出，未唤醒则往下尝试。
        **/
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }
        /** 
         将当前放入等待队列
        **/
        final boolean transferForSignal(Node node) {
            if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
                return false;
            Node p = enq(node);
            int ws = p.waitStatus;
            if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
                LockSupport.unpark(node.thread);
            return true;
        }
```