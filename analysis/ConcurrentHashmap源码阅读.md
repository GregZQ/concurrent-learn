**ConcurrentHashMap原理：  **
 
ConcurrentHashMap内部通过一个Node<K,V>[]数组来保存添加到map中的键值对，而在同一个数组位置是通过链表和红黑树的形式保存。这个数组只有在第一次添加元素的时候才会初始化，否则只是初始化一个ConcurrentHashMap对象的话，只是设定了一个sizeCtl变量，这个变量用来判断对象的一些状态和是否需要扩容。 
第一次添加元素的时候，默认初期长度为16，当往map中继续添加元素的时候，通过hash值跟数组长度取与来决定放在数组的哪个位置，如果出现放在同一个位置的时候，优先以链表的形式存放，在同一个位置的个数又达到了8个以上，如果数组的长度还小于64的时候，则会扩容数组。如果数组的长度大于等于64了的话，在会将该节点的链表转换成树。

通过扩容数组的方式来把这些节点给分散开。然后将这些元素复制到扩容后的新的数组中，同一个链表中的元素通过hash值的数组长度位来区分，是还是放在原来的位置还是放到扩容的长度的相同位置去 。在扩容完成之后，如果某个节点的是树，同时现在该节点的个数又小于等于6个了，则会将该树转为链表。  

取元素的时候，相对来说比较简单，通过计算hash来确定该元素在数组的哪个位置，然后在通过遍历链表或树来判断key和key的hash，取出value值。

**重要属性: **
```   
    //默认的数组长度
    private static final int DEFAULT_CAPACITY = 16;
    
    /** 
     负载因子。  当数组元素数量为 数组长度*负载因子的时候。
                 就需要进行数组的扩容
     
    **/
    private static final float LOAD_FACTOR = 0.75f;
    /** 
      链表转换成树的阈值
    **/
    static final int TREEIFY_THRESHOLD = 8;
    /** 
      由树转换为链表的阈值
    **/
    static final int UNTREEIFY_THRESHOLD = 6;
    
    /** 
      链表转化为树时候数组阈值
    **/
    static final int MIN_TREEIFY_CAPACITY = 64;
    
    /**  
       多线程扩容的时候单个线程负责的table的元素个数
    **/
    private static final int MIN_TRANSFER_STRIDE = 16;

    //ForwardNode的hash值。ForwardNode是一种临时节点，表示正在进行扩容操作
    static final int MOVED     = -1; 
    //TreeBin的hash值
    static final int TREEBIN   = -2; 
    //ReservationNode的hash值
    static final int RESERVED  = -3; 
    //
    static final int HASH_BITS = 0x7fffffff; 
    //CPU的核心数，用于在扩容时计算一个线程一次要干多少活
    static final int NCPU = Runtime.getRuntime().availableProcessors();  
    //操作的数组
    transient volatile Node<K,V>[] table;
    //在扩容时候使用的数组
    private transient volatile Node<K,V>[] nextTable; 
    
    //sizeCtl = -1，表示有线程正在进行初始化操作，防止多线程同时初始化Map  
    //sizeCtl = -(1 + nThreads)，表示有nThreads个线程正在进行扩容操作  
    //sizeCtl > 0，表示接下来的初始化操作中的Map容量，或者表示初始化/扩容完成后的阈值
    //sizeCtl = 0，默认值
    private transient volatile int sizeCtl;
    
    //用于多线程一起扩容的时候记录位置
    private transient volatile int transferIndex;
   
```    
**节点**  

`Node节点：`  
```java  
   
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        volatile V val;    //定义为volatile，配合CAS一起使用
        volatile Node<K,V> next;

        Node(int hash, K key, V val, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey()       { return key; }
        public final V getValue()     { return val; }
        public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
        public final String toString(){ return key + "=" + val; }
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            Object k, v, u; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }
        //查询指定值的方法   
        Node<K,V> find(int h, Object k) {
            Node<K,V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }   

```  
**TreeNode节点**  
  
  用于构造红黑树  
  
**ForwardingNode**    
ForwardingNode是一种临时节点，在扩容进行中才会出现，hash值固定为-1，并且它不存储实际的数据数据。如果旧数组的一个hash桶中全部的节点都迁移到新数组中，旧数组就在这个hash桶中放置一个ForwardingNode。读操作或者迭代读时碰到ForwardingNode时，将操作转发到扩容后的新的table数组上去执行，写操作碰见它时，则尝试帮助扩容。
```java  
    static final class ForwardingNode<K,V> extends Node<K,V> {
        final Node<K,V>[] nextTable;
        ForwardingNode(Node<K,V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        Node<K,V> find(int h, Object k) {
        
            outer: for (Node<K,V>[] tab = nextTable;;) {
                Node<K,V> e; int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                    (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (;;) {
                    int eh; K ek;
                    if ((eh = e.hash) == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K,V>)e).nextTable;
                            continue outer;
                        }
                        else
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }  
```

**三个CAS方法**   
  通过他们可以实现在非阻塞的情况下通过线程安全的方式插入修改节点
```java
 
//获取i位置上的节点
static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
    return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
}
//CAS设置i位置上的节点为v
static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                    Node<K,V> c, Node<K,V> v) {
    return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}
//利用volatile方法设置i位置的值 
static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
    U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
}
```   

**构造方法**   
```java  
    //使用默认的大小的数据创建数组（延迟化加载，在第一次put的时候才创建）
    public ConcurrentHashMap() {
    }
    /**
          initialCapacity:初始化数组大小
         loadFactor：负载因子
         concurrencyLevel：同时更新线程的估计数
         后两个参数实际没有什么具体作用，为了兼容之前的版本。 现在只是用于计算数组初始容量
    **/
    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   
            initialCapacity = concurrencyLevel; 
        //初始化大小为传入initialCapacity与负载因子相除对应的最近的二的n次方
        //作用是将节点对应的哈希码均匀分布在数组当中
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
            MAXIMUM_CAPACITY : tableSizeFor((int)size);
        //将数组初始容量赋予sizeCtl
        this.sizeCtl = cap;
    }
    
    //找到大于c的最近的2的x次方的数返回
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
```

**真正初始化方法**  
```java
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
             /** 
                 如果sizeCtl的值小于0，
                 表示有其他线程正在进行扩容或者初始化操作。
                 让出cpu资源。
             **/     
            if ((sc = sizeCtl) < 0)
                Thread.yield();
            //通过cas尝试将sizeCtl的值更新为-1,表示获取到初始化数组的权利。  
            //然后初始化数组，并将sizeCtl更新为初始值的0.75倍
            //如果失败表明其他线程已经进行了初始化操作
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }
```

**插入元素操作**     
    
    流程：   1. 计算出节点的hash值。通过hash值得到应该放在数组的哪个位置  
             2. 将其添加到链表或者红黑树的末尾   
             3. 如果在添加过程当中数组正在扩容则先帮助进行扩容  
             4. 如果添加的是链表而且链表长度为8则尝试扩容或者链表对应的数组转化为红黑树  
```java  
    public V put(K key, V value) {
        return putVal(key, value, false);
    }
    
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        //获取hash值，如果hash值为负的转换为正的
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                //初始化数组
                tab = initTable();
                //获取指定hash位置，如果指定位置为空，通过cas操作将其修改为新的值
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            //如果发现头结点为MOVED节点,表示现在正在进行扩容操作，帮助进行扩容
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
            //否则就表明当前数组位置已经有值。需要将值添加到数组对应的链表/红黑树的末尾
            //如果对应的key相同，则根据onlyIfAbsent选择是否将value替换为新值
            //注意这里锁住的只是f一个桶。从而提高了并发效率
                V oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            //通过binCount进行计数
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        //如果f是红黑树节点
                        //则将其添加到红黑树当中
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                //判断binCount的值是否达到了链表转化为红黑树的阈值。如果达到的话
                //将节点转化为红黑树节点
                /**  
                   链表转化为红黑树的条件：  
                      1. 数组长度大于64
                      2. 相应数组位置对应的链表长度大于等于8
                **/
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }    
```  
**帮助扩容**   
```  
   final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
    Node<K,V>[] nextTab; int sc;
    // 如果 table 不是空 且 node 节点是转移类型，数据检验
    // 且 node 节点的 nextTable（新 table） 不是空，同样也是数据校验
    // 尝试帮助扩容
    if (tab != null && (f instanceof ForwardingNode) &&
        (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
        // 根据 length 得到一个标识符号
        int rs = resizeStamp(tab.length);
        // 如果 nextTab 没有被并发修改 且 tab 也没有被并发修改
        // 且 sizeCtl  < 0 （说明还在扩容）
        while (nextTab == nextTable && table == tab &&
               (sc = sizeCtl) < 0) {
            // 如果 sizeCtl 无符号右移  16 不等于 rs （ sc前 16 位如果不等于标识符，则标识符变化了）
            // 或者 sizeCtl == rs + 1  （扩容结束了，不再有线程进行扩容）（默认第一个线程设置 sc ==rs 左移 16 位 + 2，当第一个线程结束扩容了，就会将 sc 减一。这个时候，sc 就等于 rs + 1）
            // 或者 sizeCtl == rs + 65535  （如果达到最大帮助线程的数量，即 65535）
            // 或者转移下标正在调整 （扩容结束）
            // 结束循环，返回 table
            if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                sc == rs + MAX_RESIZERS || transferIndex <= 0)
                break;
            // 如果以上都不是, 将 sizeCtl + 1, （表示增加了一个线程帮助其扩容）
            if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                // 协助进行扩容
                transfer(tab, nextTab);
                // 结束循环
                break;
            }
        }
        return nextTab;
    }
    return table;
}
```  

**核心--尝试扩容方法**   
   在调用扩容方法之前，先调用尝试扩容方法     
```java  
    // 首先要说明的是，方法参数 size 传进来的时候就已经翻了倍了
private final void tryPresize(int size) {
    // c：size 的 1.5 倍，再加 1，再往上取最近的 2 的 n 次方。
    int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
        tableSizeFor(size + (size >>> 1) + 1);
    int sc;
    while ((sc = sizeCtl) >= 0) {
        Node<K,V>[] tab = table; int n;
 
        // 这个 if 分支是用于初始化数组
        if (tab == null || (n = tab.length) == 0) {
            n = (sc > c) ? sc : c;
            if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if (table == tab) {
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = nt;
                        sc = n - (n >>> 2); // 0.75 * n
                    }
                } finally {
                    sizeCtl = sc;
                }
            }
        }
        else if (c <= sc || n >= MAXIMUM_CAPACITY)
            break;
        else if (tab == table) {
            // 我没看懂 rs 的真正含义是什么，不过也关系不大
            int rs = resizeStamp(n);
 
            if (sc < 0) {
                Node<K,V>[] nt;
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                // 2. 用 CAS 将 sizeCtl 加 1，然后执行 transfer 方法
                //    此时 nextTab 不为 null
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    transfer(tab, nt);
            }
            // 1. 将 sizeCtl 设置为 (rs << RESIZE_STAMP_SHIFT) + 2)
            //     我是没看懂这个值真正的意义是什么？不过可以计算出来的是，结果是一个比较大的负数
            //  调用 transfer 方法，此时 nextTab 参数为 null
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                transfer(tab, null);
        }
    }
}
```

**核心---扩容方法**    

流程：  
   1. 通过计算 CPU 核心数和 Map 数组的长度得到每个线程（CPU）要帮助处理多少个桶，并且这里每个线程处理都是平均的。默认每个线程处理 16 个桶。因此，如果长度是 16 的时候，扩容的时候只会有一个线程扩容。  
   2. 初始化临时变量 nextTable。将其在原有基础上扩容两倍。
   3. 死循环开始转移。多线程并发转移就是在这个死循环中，根据一个 finishing 变量来判断，该变量为 true 表示扩容结束，否则继续扩容。
     3.1 进入一个 while 循环，分配数组中一个桶的区间给线程，默认是 16. 从大到小进行分配。当拿到分配值后，进行 i-- 递减。这个 i 就是数组下标。（其中有一个 bound 参数，这个参数指的是该线程此次可以处理的区间的最小下标，超过这个下标，就需要重新领取区间或者结束扩容，还有一个 advance 参数，该参数指的是是否继续递减转移下一个桶，如果为 true，表示可以继续向后推进，反之，说明还没有处理好当前桶，不能推进)  
     3.2 while 循环，进 if 判断，判断扩容是否结束，如果扩容结束，清空临死变量，更新 table 变量，更新库容阈值。如果没完成，但已经无法领取区间（没了），该线程退出该方法，并将 sizeCtl 减一，表示扩容的线程少一个了。如果减完这个数以后，sizeCtl 回归了初始状态，表示没有线程再扩容了，该方法所有的线程扩容结束了。（这里主要是判断扩容任务是否结束，如果结束了就让线程退出该方法，并更新相关变量）。然后检查所有的桶，防止遗漏。  
     3.3 如果没有完成任务，且 i 对应的槽位是空，尝试 CAS 插入占位符，让 putVal 方法的线程感知。  
     3.4 如果 i 对应的槽位不是空，且有了占位符，那么该线程跳过这个槽位，处理下一个槽位。  
     3.5 如果以上都是不是，说明这个槽位有一个实际的值。开始同步处理这个桶。  
     3.6 到这里，都还没有对桶内数据进行转移，只是计算了下标和处理区间，然后一些完成状态判断。同时，如果对应下标内没有数据或已经被占位了，就跳过了。  
   4.处理每个桶的行为都是同步的。防止 putVal 的时候向链表插入数据。
     4.1 如果这个桶是链表，那么就将这个链表根据 length 取于拆成两份，取于结果是 0 的放在新表的低位，取于结果是 1 放在新表的高位。   
     4.2 如果这个桶是红黑数，那么也拆成 2份，方式和链表的方式一样，然后，判断拆分过的树的节点数量，如果数量小于等于6，改造成链表。反之，继续使用红黑树结构。  
     4.3 到这里，就完成了一个桶从旧表转移到新表的过程。    
```java  
    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        //确定每个cpu需要负责的桶的个数   
        //如果每个cpu分配的桶的个数小于16的话，则每个cpu处理16个数
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        if (nextTab == null) {            // initiating
            try {
                //将新数组设为原数组的两倍
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                //更新
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                //扩容失败，sizeCtl使用int最大值
                sizeCtl = Integer.MAX_VALUE;
                return;//结束
            }
            //更新成员变量
            nextTable = nextTab;
            //更新转义下标，也就是老的table的length
            transferIndex = n;
        }
        int nextn = nextTab.length;
        //新的节点，表示正在占位
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
        // 首次推进为 true，如果等于 true，说明需要再次推进一个下标（i--），
        //反之，如果是false，那么就不能推进下标，需要将当前的下标处理完毕才能继续推进
        boolean advance = true;
        //完成状态，如果是true，就结束此方法
        boolean finishing = false; // to ensure sweep before committing nextTab
        // 死循环,i 表示下标，bound 表示当前线程可以处理的当前桶区间最小下标
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;
            // 如果当前线程可以向后推进；这个循环就是控制 i 递减。同时，每个线程都会进入这里取得自己需要转移的桶的区间
            while (advance) {
                int nextIndex, nextBound;
                /**
              对 i 减一，判断是否大于等于 bound （正常情况下，如果大于 bound不成立，说明该线程上次领取的任务已经完成了。那么，需要在下面继续领取任务）    
              如果对 i 减一大于等于 bound（还需要继续做任务），或者完成了，修改推进状态为 false，不能推进了。任务成功后修改推进状态为 true。    
             通常，第一次进入循环，i-- 这个判断会无法通过，从而走下面的nextIndex赋值操作（获取最新的转移下标）。其余情况都是：如果可以推进，将i减一，然后修改成不可推进。如果 i 对应的桶处理成功了，改成可以推进。                
                **/
                if (--i >= bound || finishing)
                // 这里设置 false，是为了防止在没有成功处理一个桶的情况下却进行了推进
                    advance = false;
                /**  
                这里的目的是：  
                1. 当一个线程进入时，会选取最新的转移下标。
                2. 当一个线程处理完自己的区间时，如果还有剩余区间的没有别的线程处理。再次获取区间。
                **/
                else if ((nextIndex = transferIndex) <= 0) {
                // 如果小于等于0，说明没有区间了 ，i 改成 -1，推进状态变成 false，不再推进，表示，扩容结束了，当前线程可以退出了
                // 这个 -1 会在下面的 if 块里判断，从而进入完成状态判断
                    i = -1;
                    advance = false;
                }
                // CAS 修改 transferIndex，即 length - 区间值，留下剩余的区间值供后面的线程使用
                else if (U.compareAndSwapInt
                         (this, TRANSFERINDEX, nextIndex,
                          nextBound = (nextIndex > stride ?
                                       nextIndex - stride : 0))) {
                    // 这个值就是当前线程可以处理的最小当前区间最小下标                   
                    bound = nextBound;
                     // 初次对i 赋值，这个就是当前线程可以处理的当前区间的最大下标
                    i = nextIndex - 1;
                    // 这里设置 false，是为了防止在没有成功处理一个桶的情况下却进行了推进，这样对导致漏掉某个桶。下面的 if (tabAt(tab, i) == f)判断会出现这样的情况。
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                /**
                   表示扩容完成
                **/
                if (finishing) {
                    nextTable = null;
                    //将数组指向新的数组
                    table = nextTab;
                    //sizeCtl的值为新数组的0.75倍
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                //利用CAS方法更新这个扩容阈值，在这里面sizectl值减一，说明新加入一个线程参与到扩容操作
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            }
            // 获取老 tab i 下标位置的变量，如果是 null，就使用 fwd 占位。
            else if ((f = tabAt(tab, i)) == null)
            // 如果成功写入 fwd 占位，再次推进一个下标    
                advance = casTabAt(tab, i, null, fwd);
            // 如果不是 null 且 hash 值是 MOVED。
            else if ((fh = f.hash) == MOVED)
            // 说明别的线程已经处理过了，再次推进一个下标
                advance = true; 
            else {
            // 到这里，说明这个位置有实际值了，且不是占位符。对这个节点上锁。为什么上锁，防止 putVal 的时候向链表插入数据
                synchronized (f) {
                // 判断 i 下标处的桶节点是否和 f 相同
                    if (tabAt(tab, i) == f) {
                        //高位桶，低位桶
                        Node<K,V> ln, hn;
                        // 如果 f 的 hash 值大于 0 。TreeBin 的 hash 是 -2
                        if (fh >= 0) {
                            /** 
                              fh与n相与，最后的出来肯定要么是0，要么是1. 而1就放在高位桶中存储
                              0就放在低位桶中存储
                            **/
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
                            //找到最后一个节点的runBit值并将其赋值给lastRun
                            //目的：优化数组迁移，如果后面数组的值都是在一个桶当中，那么可以直接将lastRun对应的链表
                            //放入到对应桶中
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            else {
                                hn = lastRun;
                                ln = null;
                            }
                            //逆向将链表元素放入到对应桶当中
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }
                            //放入到低位桶上
                            setTabAt(nextTab, i, ln);
                            //放入到高位桶上
                            setTabAt(nextTab, i + n, hn);
                            // 将旧的链表设置成占位符
                            setTabAt(tab, i, fwd);
                            //当前桶更新完成，向前推进
                            advance = true;
                        }
                        else if (f instanceof TreeBin) {
                            //红黑树方法与上面链表类似
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> lo = null, loTail = null;
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K,V> p = new TreeNode<K,V>
                                    (h, e.key, e.val, null, null);
                                // 和链表相同的判断，与运算 == 0 的放在低位
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                //不是0的放在高位
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            // 如果树的节点数小于等于 6，那么转成链表，反之，创建一个新的树
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                (lc != 0) ? new TreeBin<K,V>(hi) : t;
                            // 低位树
                            setTabAt(nextTab, i, ln);
                            //高位树
                            setTabAt(nextTab, i + n, hn);
                            //设置占位符
                            setTabAt(tab, i, fwd);
                            //向前推进
                            advance = true;
                        }
                    }
                }
            }
        }
    }    
```    
**get方法比较简单不进行分析**
    
