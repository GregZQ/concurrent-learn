    
  AQS类似于一个框架。作为J.U.C常用并发工具的基本基类。它维护了一个state(核心)，一个列表。AQS有两套维护共享状态的方式：独占，共享。也就是说，如果你自己想写一个基于AQS的并发类，你只需要重写其中一种（独占或共享）访问资源的方式即可。另外对于两套共享资源的方式又可分为可中断的与不可中断的，两者区别就是可中断可以抛出一个中断异常，其他基本一样，所以我只阅读了可中断的。   
  并且ASQ通过一个CAS费组社方式+前继唤醒后继节点的操作，防止了每个节点轮询访问获取资源，减少了性能消耗。   
  
总流程：   
   
   AQS维护一个同步队列。 每个线程在AQS当中被封装为一个节点，当节点尝试获取资源的时候，如果成功获取资源就执行代码其他操作，如果失败则会尝试通过一次CAS操作获取资源。如果这时候还不成功就会将其放入同步队列当中阻塞，等待前继节点执行完操作之后释放资源将其唤醒
  
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
        //表示节点对应的线程处于无效状态
        static final int CANCELLED =  1;
        //被标识为该等待唤醒状态的后继结点，当前面节点的资源被释放后，通知它被唤醒
        static final int SIGNAL    = -1;
        //表示当前线程是在条件队列当中等待
        static final int CONDITION = -2;
        //与共享模式相关，在共享模式中，该状态标识结点的线程处于可运行状态。
        static final int PROPAGATE = -3;
        //另外还有一个0状态，不在上面几种之内，代表初始化状态。
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
       获取独占资源，如果尝试获取资源没有则执行后面的代码
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
     这时候通过非阻塞自旋操作将当前结点放入队列末尾。
     并返回当前节点
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
      当将结点放入队列后，会先尝试获取资源，
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
                如果前一个为头结点，则自身再次尝试获取资源。如果成功
                则将当前结点设置为头结点。并返回当前节点是否中断的状态。
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
                  shouldParkAfterFailedAcquire方法。该方法做了三个工作：
        -         确定后继是否需要park;
        -         跳过被取消的结点;
        -         设置前继的waitStatus为SIGNAL.
                 如果返回true，则将当前节点设置为阻塞状态，等待其他人唤醒。
                 并检测阻塞过程当中是否被中断过，返回中断与否的状态。
                **/
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
        //如果代码执行过程当中当前节点出现异常，则将当前节点从队列当中移除
            if (failed)
                cancelAcquire(node);
        }
    }
    
    /**
-         确定后继是否需要park;
-         跳过被取消的结点;
-         设置前继的waitStatus为SIGNAL.
    **/
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        //前继节点准备好park后面的节点了，所以后面节点可以进入park状态。
        if (ws == Node.SIGNAL)
            return true;
        //前继节点未准备好PARK后面的节点。当前节点再次尝试重新获取资源
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
        当进入这个方法是会真的被挂起，但是这个阻塞是不会相应中断抛出异常，
        但中断标志位会被设定。所以需要检查一下中断标志位的状态。
    **/
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }
    /**
      将当前节点的状态设置为无效状态。
      通过CAS尝试将当前节点从等待队列当中移除。
      如果当前节点为头节点，则唤醒后继队列当中第一个有效的节点
    **/
    private void cancelAcquire(Node node) {
        if (node == null)
            return;
        node.thread = null;
        //协助将等待队列当中的无效节点移除
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        Node predNext = pred.next;
        //设定当前节点为无效状态
        node.waitStatus = Node.CANCELLED;
        //如果节点为尾节点，则通过cas操作将pre节点的尾节点设置为空。表示pre节点成为尾节点
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            //如果是中间节点，尝试通过cas操作将当前节点从队列当中移除
            int ws;
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                //如果它是头节点，则将后面的第一个有效节点唤醒
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }    
```
#### 独占方式释放资源
```java
   /**
     先尝试释放资源调用tryRelease方法。这个方法需要人员自己实现，如果
     获取释放成功则调用unparkSuccessor方法，唤醒当队列当中第一个后继节点，尝试获取资源。
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
       唤醒后继第一个有效节点
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
     尝试获取共享资源
    **/
    private void doAcquireShared(int arg) {
       //创建一个共享的结点放到队列的尾部
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                /**
                  如果当前结点是头结点的下一个，则尝试获取资源（可能头结点获取的资源已经被释放）。
                **/
                final Node p = node.predecessor();
                if (p == head) {
                   
                    int r = tryAcquireShared(arg);
                    //如果获取资源成功，则出队。
                    //会往后面结点传播唤醒的操作，保证剩下等待的线程能够尽快 获取到剩下的许可。
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
                     如果确定当前Node需要被park，则进行park。
                     并返回阻塞过程当中的中断状态。
                      
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

//独占与贡献的区别就在于共享的线程在获取资源后，如果还有资源可以获取则会唤醒后继节点
    private void setHeadAndPropagate(Node node, int propagate) {
        //将当前节点设置为头结点
        Node h = head; 
        setHead(node);
        //尝试唤醒后继节点  
        /**
         1.如果propagate>0，说明许可还能够被后继节点获取 
         2. h.waitStatus<0,说明需要向后传递  
         3. h为空。不确定什么情况
        **/
        if (propagate > 0 || h == null || h.waitStatus < 0) {
            Node s = node.next;
            // 后继结是共享模式或者s == null（不知道什么情况）
           // 如果后继是独占模式，那么即使剩下的许可大于0也不会继续往后传递唤醒操作
           // 即使后面有结点是共享模式。
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }
    
    //尝试唤醒后继节点
    private void doReleaseShared() {
        for (;;) {
            Node h = head;
            // 队列不为空且有后继结点
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                // 不管是共享还是独占只有结点状态为SIGNAL才尝试唤醒后继结点
                if (ws == Node.SIGNAL) {
                    // 将waitStatus设置为0
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue; // loop to recheck cases
                    unparkSuccessor(h);// 唤醒后继结点
                    // 如果状态为0则更新状态为PROPAGATE，更新失败则重试
                } else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue; // loop on failed CAS
            }
            // 如果过程中head被修改了则重试，否则在唤醒之后则退出。
            if (h == head) // loop if head changed
                break;
        }
}
```
#### 共享方式释放资源
```java
    /**
      释放共享资源，释放后调用doReleaseShared方法唤醒后继。
    **/
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }
```
#### 关于ConditionObject，  

Condition相关内容是为了实现await，singal，singalAll条件队列。  
Condition维护了一个链表。链表当中的节点表示的线程处于await（等待）状态。     

整体流程：当一个线程进入await状态的时候，会将其自身阻塞，释放资源并将其放入Conditon维护的队列当中去。   
          当其他线程调用signal操作的时候，会将放在条件队列当中的节点放入到获取资源的同步队列当中去。等待同步队列当中的前继节点将其唤醒。   
          当调用signalAll操作的时候，会将放在条件队列当中的所有节点放入到同步队列当中去。等待被唤醒
    

```java
        /** 
            用于维护await后挂起的结点列表
        **/
        private transient Node firstWaiter;
        private transient Node lastWaiter;
        
        /** 表示这是一个普通被唤醒的节点 **/
        private static final int REINTERRUPT =  1;
        /** 表示这是一个被中断的节点，不是正常被唤醒 **/
        private static final int THROW_IE    = -1;
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
            int interruptMode = 0;
            /**
              判断当前队列是否在获取资源的队列当中。如果不在，则挂起
              为什么会有这样一个判断？  
              因为在fullyRelease之后，表明当前节点已经释放了了所有的资源。这时候可能已经有另外一个
              线程执行signal方法，将当前的节点已经放入到等待队列当中等待。这时候就不需要进入park状态
            **/
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                /** 
                当到这一步后，表示结点被唤醒，然后调用
                checkInterruptWhileWaiting判断结点是
                如何被唤醒的，singal或发生InterruptException异常。
                **/
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            /**
             到这将结点加入等待获取资源的队列当中，当获取到资源后才会退出await
             并将结点从条件队列中移除，用
             reportInterruptAfterWait反馈当前结点被唤醒的状态。
             **/
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) 
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }
        //将当前节点添加到条件队列当中
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            //如果最后一个节点不是处于条件队列当中的等待状态
            //则调用unlinkCancelledWaiters方法将条件队列当中
            //不是出于等待状态的节点移除
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            //将当前节点加入到条件队列当中
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }
        //将条件队列当中不是处于条件队列等待状态的节点移除
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    trail = t;
                t = next;
            }
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
            //如果不是当前锁的持有者，抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            //唤醒队列当中的第一个节点，并将其从等待队列当中移除
            if (first != null)
                doSignal(first);
        }
        /**
        具体的唤醒操作，调用transferForSignal方法，尝试去唤醒头结
        点，唤醒成功则退出，未成功表示已经有其他线程调用的singal方法
        唤醒了这个节点，它需要继续向下寻找节点唤醒。
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
           1.如果返回false表明已经有其他线程调用了singal方法。
           此时需要上层再次调用doSingal方法重新获取一个处于
           等待队列的节点  
           2. 如果设置成功，将当前节点放到等待队列的末尾。
           等待重新被唤醒获取资源进行操作
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

#####  signalAll:唤醒等待队列当中所有的节点  

```java
    //释放在条件队列当中所有的节点  
        public final void signalAll() {
           //如果不是当前锁的持有者，抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                //释放所有的条件队列当中的节点
                doSignalAll(first);
        }
        
    //释放所有节点，将节点全部放入等待队列当中等待获取资源    
    private void doSignalAll(Node first) {
        lastWaiter = firstWaiter = null;
        do {
            Node next = first.nextWaiter;
            first.nextWaiter = null;
            transferForSignal(first);
            first = next;
        } while (first != null);
    }
```