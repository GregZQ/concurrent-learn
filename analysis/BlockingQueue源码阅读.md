##### 一、什么是阻塞队列   
   阻塞队列是J.U.C包中的内容。常用于实现生产者消费者模式。  
   它维护的是这样一个队列：  
   
     当一个线程向阻塞队列当中添加元素的时候，如果队列未满就可以继续添加，知道队列满。这时候再添加会阻塞住  
    当另外一个线程从阻塞队列当中取数据的时候，如果队列有元素就取出。如果未空则阻塞住    
    
    
##### 二、继承关系    
   
   BlockQueue继承自Queue. 而ArrayBlockingQueue、LinkedBlockingQueue、SynchronousQueue、PriorityBlockingQueue四个分别是它的实现。   
   
类名|描述  
:--:|:--:  
ArrayBlockingQueue|底层是数组的阻塞队列  
LinkedBlockingQueue|底层是链表的阻塞队列  
SynchronousQueue|队列当中只能有一个元素的阻塞队列,每个插入操作必须等待另一个线程的对应移除操作   
PriorityBlockingQueue|队列当中元素出队顺序按照某种优先级排列的队列    

##### 三、相关方法   

操作|失败抛出异常|返回true/false|阻塞|超时   
:--:|:--:|:--:|:--:|:--:  
插入|add(e)|offer(e)|put(e)|offer(e,time,unit) 
*|**返回true/false**|**返回头元素或者null**|**阻塞**|**超时**
移除|remove(e)|poll|take|poll(time,unit)    

add(e):将指定的元素插入到此队列的尾部（如果立即可行且不会超过该队列的容量），在成功时返回 true，如果此队列已满，则抛出 IllegalStateException。  
offer(e):将指定的元素插入到此队列的尾部（如果立即可行且不会超过该队列的容量），在成功时返回 true，如果此队列已满，则返回 false。  
put(e): 将指定的元素插入此队列的尾部，如果该队列已满，则等待可用的空间。 
offer(e,time,unit):将指定的元素插入此队列的尾部，如果该队列已满，则在到达指定的等待时间之前等待可用的空间。  
remove:从此队列中移除指定元素的单个实例（如果存在）。 
poll:获取并移除此队列的头，如果此队列为空，则返回 null。  
take: 获取并移除此队列的头部，在元素变得可用之前一直等待（如果有必要）。  
poll(time,unit): 获取并移除此队列的头部，在指定的等待时间前等待可用的元素（如果有必要）。     

##### 四、ArrayBlockQueue   

`属性:`  
```java
    private final E[] items;//底层数据结构
    private int takeIndex;//用来为下一个take/poll/remove的索引（出队）
    private int putIndex;//用来为下一个put/offer/add的索引（入队）
    private int count;//队列中元素的个数

    private final ReentrantLock lock;//锁
    private final Condition notEmpty;//等待出队的条件
    private final Condition notFull;//等待入队的条件
```  

`构造方法：`
```java
    
    //capacity:存储元素数组的大小
    //fair:创建的锁是安全还是不安全的
    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull =  lock.newCondition();
    }
```  
`添加元素方法:`     
```
    //add内部调用super.add
    public boolean add(E e) {
        return super.add(e);
    }
    //super.add内部调用offer。
    //添加成功返回true.否则抛出队列满的异常
    public boolean add(E e) {
        if (offer(e))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }
    
    /**
      用独占锁的方式插入元素。
      1.如果插入成功返回true 
      2.否则返回false
    **/
    public boolean offer(E e) {
        //检查插入数据是否为空，空的话抛出空指针异常
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            //如果队列已满返回false
            if (count == items.length)
                return false;
            else {
            //将元素入队
                enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     元素入队。做了三件事 
     1. 将元素放入数组当中
     2. putIndex+1,表示下一个插入元素文职
     3. 释放等待队列非空的信号，使获取元素的线程被唤醒
    **/
    private void enqueue(E x) {
        final Object[] items = this.items;
        items[putIndex] = x;
        if (++putIndex == items.length)
            putIndex = 0;
        count++;
        notEmpty.signal();
    }
    
    /** 
       阻塞的方式插入元素：如果队列满了就等待  
       
       1.调用可中断的锁
       2. 判断一下队列是否满了，如果满了则等待被唤醒
       3. 未满就将当前元素放入队列末尾
    **/
    public void put(E e) throws InterruptedException {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length)
                notFull.await();
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }
     
```    
`删除元素:`
```java
    /**
      元素删除：删除成功返回true。否则返回false
    **/
    public boolean remove(Object o) {
        if (o == null) return false;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                final int putIndex = this.putIndex;
                //从元素头位置开始遍历
                int i = takeIndex;
                do {
                    //找到元素匹配的话，删除指定位置元素
                    if (o.equals(items[i])) {
                        removeAt(i);
                        return true;
                    }
                    if (++i == items.length)
                        i = 0;
                } while (i != putIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }  
    
    /** 
      删除指定位置的元素。
      并释放队列非满的信号。
      
    **/
    void removeAt(final int removeIndex) {
        final Object[] items = this.items;
        //如果删除的是头节点（takeIndex），只需将takeIndex增一。
        if (removeIndex == takeIndex) {
            items[takeIndex] = null;
            if (++takeIndex == items.length)
                takeIndex = 0;
            count--;
            if (itrs != null)
                itrs.elementDequeued();
        } else {
            //如果删除的是中间节点，则需要将后面的节点往前挪一位
            final int putIndex = this.putIndex;
            for (int i = removeIndex;;) {
                int next = i + 1;
                if (next == items.length)
                    next = 0;
                if (next != putIndex) {
                    items[i] = items[next];
                    i = next;
                } else {
                    items[i] = null;
                    this.putIndex = i;
                    break;
                }
            }
            count--;
            if (itrs != null)
                itrs.removedAt(removeIndex);
        }
        notFull.signal();
    }    
    
    /** 
    删除头元素,如果队列为空则返回null
    **/
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }
    
    /** 
    从队列当中取出头元素，并将元素数量减一
    并释放非满信号
    **/
    private E dequeue() {
        final Object[] items = this.items;
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length)
            takeIndex = 0;
        count--;
        if (itrs != null)
            itrs.elementDequeued();
        notFull.signal();
        return x;
    }
    /**
      在指定时间到后返回。如果此时队列为空返回null。
      如果队列不为空移出头结点。
    **/
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        //通过可中断锁的方式锁住
        lock.lockInterruptibly();
        try {
            //如果队列为空就进入等待状态
            while (count == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            //如果在规定时间内队列当中有元素并且当前线程处于唤醒状态就将头元素返回。
            return dequeue();
        } finally {
            lock.unlock();
        }
    }    
 
    /** 
    如果队列为空就进入阻塞状态。直到队列当中有元素并且它被唤醒就将头元素取出
    **/
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0)
                notEmpty.await();
            return dequeue();
        } finally {
            lock.unlock();
        }
    }   
```
`其他方法：`  
```java
    /**
      取出头元素。
    **/
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return itemAt(takeIndex);
        } finally {
            lock.unlock();
        }
    }
    /** 
     返回数组指定位置的元素。如果位置元素为空返回null
    **/
    final E itemAt(int i) {
        return (E) items[i];
    }

```   
##### 五、LinkedBlockQueue  
  
  LinkedBlockQueue内部使用链表的形式存储数据。  这里只与ArrayBlockQueue对比找出不同的地方。  

从这里面可以发现，LinkedBlockQueue将取数据的锁与放入数据的锁分开。从而减少了锁的竞争。从而提高了效率。  
`属性：`
```
    //用一个节点储存值以及对应的下一个节点
    static class Node<E> {
        E item;

        Node<E> next;

        Node(E x) { item = x; }
    }
    
    //用于标记头元素
    transient Node<E> head;
    //用于标记尾元素
    private transient Node<E> last;    
    //取头元素的锁
    private final ReentrantLock takeLock = new ReentrantLock();
    //非空条件
    private final Condition notEmpty = takeLock.newCondition();
    //放元素的锁
    private final ReentrantLock putLock = new ReentrantLock();
    /**非满条件**/
    private final Condition notFull = putLock.newCondition();
```  
##### 六、PriorityBlockingQueue   
优先队列内部维护的是一个可以自动增长的数组。它通过内部优先级决定元素在数组当中的排列顺序。      

内部维护的数组使用堆排序，将优先级最大的放在队列头上。     

`插入元素：`   
```java
 /** 内部的add、put、offer最终都调用的offer方法。
 因为队列是一个可以自动增长的数组，所以不存插入数据阻塞的问题
 **/  
    /**
       插入流程：   
       1. 判断当前需要的长度是否大于等于数据长度，如果是的话就进行扩容
       操作
       2. 通过内置comparator或者comparable进行数据比较,从而对于关系进行堆排序调整 
       3. 完成之后发送非空信号。
    **/
    public boolean offer(E e) {
        //判断队列是否为空，如果为空就抛出空指针
        if (e == null)
            throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        int n, cap;
        Object[] array;
        //如果需要的长度大于当前数据的长度就进行尝试扩容操作
        while ((n = size) >= (cap = (array = queue).length))
            tryGrow(array, cap);
        try {
            Comparator<? super E> cmp = comparator;
            if (cmp == null)
            //如果自定义比较器为空，使用默认的方法进行比较进而进行堆排序调整
                siftUpComparable(n, e, array);
            else
            //如果自定义比较器不为空，使用比较器方法进行比较进而进行堆排序
                siftUpUsingComparator(n, e, array, cmp);
            //size加一，表示下一次添加时数组的长度
            size = n + 1;
            //释放信号，将阻塞等待获取数据的线程唤醒
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        return true;
    } 
    /** 
     尝试扩容：  
       1. 先取消锁定。因为扩容过程当中不涉及原数组的操作
       2. 通过CAS自旋的方式尝试获取扩容操作请求（如果这时候别人获取到那么只能让出cpu资源）
       3. 进行扩容： 如果远数组长度小于64，则变为原来的2倍+2.
                     大于64就变为原来1.5倍  
       4. 当扩容完成后退出扩容请求操作  
       5. 加锁进行数组的复制。复制完成后退回到上层方法继续执行相应添加操作
    **/
    private void tryGrow(Object[] array, int oldCap) {
        //扩容过程当中取消数组的锁定
        /**
          原因：因为扩容涉及到新数组，与原数组没有关系。
          取消锁定可以让其他线程继续操作而不受影响
        **/
        lock.unlock(); 
        Object[] newArray = null;
        //通过CAS操作尝试获取扩容操作请求
        if (allocationSpinLock == 0 &&
            UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset,
                                     0, 1)) {
            try {
                //进行扩容操作
                int newCap = oldCap + ((oldCap < 64) ?
                                       (oldCap + 2) : // grow faster if small
                                       (oldCap >> 1));
                if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                    int minCap = oldCap + 1;
                    if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError();
                    newCap = MAX_ARRAY_SIZE;
                }
                if (newCap > oldCap && queue == array)
                    newArray = new Object[newCap];
            } finally {
                //扩容完成后释放占用的扩容操作
                allocationSpinLock = 0;
            }
        }
        /**
        如果newArray为空，表示当前线程没有获得扩容操作（其他线程已经进行扩容操作）
        **/
        if (newArray == null) 
            Thread.yield();
        /** 
          将当前数组锁定并将原来的数组复制到新的数组当中
          将操作的数组指向新数组
          这个锁定操作会在上层方法当中释放锁的调用
        **/
        lock.lock();
        if (newArray != null && queue == array) {
            queue = newArray;
            System.arraycopy(array, 0, newArray, 0, oldCap);
        }
    }

    /**
       通过堆排序进行数据调整。
    **/
    private static <T> void siftUpComparable(int k, T x, Object[] array) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            if (key.compareTo((T) e) >= 0)
                break;
            array[k] = e;
            k = parent;
        }
        array[k] = key;
    }
```  
`取出元素:`    

取出元素poll与take类似。 所以这里只分析take方法  
```java  
   
   /**  
     1. 调用dequeue方法将数组元素取出
     2. 如果数组元素为空则释放资源等待 
   **/
   public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        E result;
        try {
            while ( (result = dequeue()) == null)
                notEmpty.await();
        } finally {
            lock.unlock();
        }
        return result;
    }
    
    /** 
      取出队列头部元素，并进行堆排序将数组进行调整  
    **/
    private E dequeue() {
        int n = size - 1;
        if (n < 0)
            return null;
        else {
            Object[] array = queue;
            E result = (E) array[0];
            E x = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            if (cmp == null)
                siftDownComparable(0, x, array, n);
            else
                siftDownUsingComparator(0, x, array, n, cmp);
            size = n;
            return result;
        }
    }

    /**  
     具体进行堆排序调整的方法
    **/
    private static <T> void siftDownComparable(int k, T x, Object[] array,
                                               int n) {
        if (n > 0) {
            Comparable<? super T> key = (Comparable<? super T>)x;
            int half = n >>> 1;           // loop while a non-leaf
            while (k < half) {
                int child = (k << 1) + 1; // assume left child is least
                Object c = array[child];
                int right = child + 1;
                if (right < n &&
                    ((Comparable<? super T>) c).compareTo((T) array[right]) > 0)
                    c = array[child = right];
                if (key.compareTo((T) c) <= 0)
                    break;
                array[k] = c;
                k = child;
            }
            array[k] = key;
        }
    }
```    



  










   
   

  
     