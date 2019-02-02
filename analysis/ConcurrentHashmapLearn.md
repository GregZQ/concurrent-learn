    /**
     * 在进行扩容操作的时候使用的坐标。
     * 可以理解为表示当前未被操作的原始数组。
     * 当一个线程在扩容时进行数据的迁移，
     * 通过它确定操作的下节。
     */
    private transient volatile int transferIndex;
    /**
     * 扩容过程中的临时数组
     */
    private transient volatile Node<K,V>[] nextTable;

    /**
     * 当数组为进行扩容操作时候为整数，而且是当前数组长度的0.75
     * 负数：表示当前数组正在进行扩容
     * -1：表示当前数组正在初始化
     * -(x)+nthread:表示当前正在进行数组的基本扩容操作。当有一个新的线程
     * 进行数据迁移操作时+1，完成后-1
     */
    private transient volatile int sizeCtl;
    /**
     * 进行数组的初始化
     * @param initialCapacity  传入的初始化的数值
     *
     *
     *  根据传入的数值确定当前数组的长度，数组的长度默认为当前1.5倍+1最近的2的幂的值
     *
     *
     *   用sizeCtl记录初始数组的长度大小
     */
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                MAXIMUM_CAPACITY :
                //
                tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    /*
    *
    * 插入元素
    * @param key  需要插入的key
    * @param value 插入的value
    * @return
    */
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    /**
     * 进行实际的数组元素插入
     * @param key 需要插入的key
     * @param value 需要插入的值
     * @param onlyIfAbsent
     * @return
     *
     *
     */
    final V putVal(K key, V value, boolean onlyIfAbsent) {//布尔值的作用，在遇见相同的时候是否替换
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());//通过hashcode获取key的hash值
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {//循环进行数据的判断
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
            tab = initTable();
            /**
             * 如果当前i位置对应的值为空，则直接插入
             */
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                        new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }

            /**
             *
             *  如果当前的f.hash值为MOVED，表示当前桶已经进行数据的迁移。
             *  直接通过helpTransfer帮助进行数据迁移，实现多线程进行数据
             *  迁移操作。
             *
             */


            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
             /*
             *  通过synchronized锁
             *  住数组中的一个桶,然后进行数据的插入
             */
            else {
                V oldVal = null;
                synchronized (f) {
                    //通过cas判断当前i位置是否是f
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {//大于等于0表示可以插入
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                               /*这个判断是否key相同*/
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)//如果当前值可以替换，则进行替换并退出循环
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                //如果当前值的next为空则进行替换
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                            value, null);
                                    break;
                                }
                            }
                        }
                        //如果f是一个红黑树结点，则进行红黑树结点的添加
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
                /**
                 * 此处通过bitCount计数进行数据的判断。
                 * 如果bitCount的数值大于等于8，则尝试转化为红黑树。
                 * 转换规则：数组长度大于64，并且当前数组某个桶的数据个数
                 * 大于8，则进行链表转为红黑树。否则先进行扩容
                 */
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

    /**
     * 进行数组的初始化
     * @return
     */
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
            //如果当前sc小于0，表示当前数组在进行扩容操作，让出cpu进行等待
            if ((sc = sizeCtl) < 0)
                Thread.yield();
            /**
             *  如果sc大于0，表示数组可以进行扩容，将当前sizectl的值
             *  设置为-1，表示在进行数组的初始化，
             *  初始化完成后，将sizeCtl设置为当前数组长度的0.75倍
             */
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

    /**
     *
     *
     * 尝试将链表转为红黑树
     * @param tab
     * @param f
     * @return
     */
    private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;
        if (tab != null) {
            //如果当前的tab长度小于64，先进行扩容
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                tryPresize(n << 1);
            //否则就将当前桶内的链表转换为红黑树
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K,V> hd = null, tl = null;
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p =
                                    new TreeNode<K,V>(e.hash, e.key, e.val,
                                            null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin<K,V>(hd));
                    }
                }
            }
        }
    }

    /**
     * 其他线程协助进行数据迁移的操作
     * @param tab  初始数组
     * @param f  桶内的头结点
     * @return
     */
    final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
        Node<K,V>[] nextTab; int sc;
        if (tab != null && (f instanceof ForwardingNode) &&
                (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
            int rs = resizeStamp(tab.length);
            while (nextTab == nextTable && table == tab &&
                    (sc = sizeCtl) < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || transferIndex <= 0)
                    break;
                //将sc的值加一
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;
        }
        return table;
}

    //初始化数组
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
            if ((sc = sizeCtl) < 0)
                Thread.yield(); //如果当前szeCtl小于0，就让出cpu
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {//通过cas设置当前SIZECTL的值
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);//将当前sizeCtl的值设置为原来的0.75
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }


    /**
     * 尝试预分配数组的长度以容纳给定尺寸的数据，
     * 但这里未进行真实的扩容
     */
    private final void tryPresize(int size) {
        //将长度变为当前的1.5倍对应的2的幂
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
                tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {//当sizeCtl大于0可以进行扩容
            Node<K,V>[] tab = table; int n;
            /**
             * 如果数组为空就进行普通的数组初始化操作，同initTale
             */
            if (tab == null || (n = tab.length) == 0) {
                //先进行数组长度的扩大
                n = (sc > c) ? sc : c;
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;//将sizeCtl变为当前的0.75
                    }
                }
            }
            //如果当前长度不合适直接结束
            else if (c <= sc || n >= MAXIMUM_CAPACITY)
                break;
            //如果当前数组没变，尝试进行扩容
            else if (tab == table) {
                //先获取一个扩容戳
                int rs = resizeStamp(n);
                /**
                 * 判断当前sc是否为负数，
                 * 如果为负数，表示正在进行数组容量的扩展。
                 * 这时候帮助其他线程进行数组扩容操作。
                 * 并将sizeCtl的数值增加1
                 */
                if (sc < 0) {
                    Node<K,V>[] nt;
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                            transferIndex <= 0)
                        break;
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);
                }
                //当前sc不是负数，将sizeCtl转为负数再进行扩容
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                        (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
            }
        }
    }

    /**
     * @param tab 原始数组
     * @param nextTab 新数组
     */
    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        //每个线程执行几个
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range

        //nextTab为空，表示首次执行扩容，先分配数组大小
        if (nextTab == null) {            // initiating
            try {
                //扩容为原来的一倍
                @SuppressWarnings("unchecked")
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }
        int nextn = nextTab.length;
        //将对应的hash值设置为-1，表示这个桶正在进行结点的迁移操作
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
        boolean advance = true;
        boolean finishing = false;
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;

            while (advance) {
                int nextIndex, nextBound;
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                }
                /**
                 *
                 * 这里需要注意一下
                 * bound与i的值。
                 * bound：这个值作为一个线程处理桶数量的上边界
                 *
                 * i：初始值等于transferindex-1;一个线程处理桶数量的下边界。
                 * 从i开始进行遍历
                 */
                else if (U.compareAndSwapInt
                        (this, TRANSFERINDEX, nextIndex,
                                nextBound = (nextIndex > stride ?
                                        nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            /**
             * 从这跳出循环
             */
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            }
            //如果i下标对应的桶为null，直接将原数组对应桶设置为moved。
            //表示当前位置已经进行数组的迁移
            else if ((f = tabAt(tab, i)) == null)
                advance = casTabAt(tab, i, null, fwd);
            //表示当前位置已经进行了数组的迁移
            else if ((fh = f.hash) == MOVED)
                advance = true; // already processed
            else {
                synchronized (f) {//将f锁住
                    /**
                     * 此处设数组长度对应的幂为x,
                     * 将原始桶内链表的元素hash值与x相与，为0的在一个链表内。
                     * 为1的在另外一个链表内。
                     * 在新数组中，相与为0的位置不变，为1的位置为i+n
                     */
                    if (tabAt(tab, i) == f) {
                        Node<K,V> ln, hn;
                        if (fh >= 0) {
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
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
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            //将
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                        else if (f instanceof TreeBin) {
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> lo = null, loTail = null;
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K,V> p = new TreeNode<K,V>
                                        (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                    (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                    (lc != 0) ? new TreeBin<K,V>(hi) : t;
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * 扩容时候的辅助结点
     * @param <K>
     * @param <V>
     */
    static final class ForwardingNode<K,V> extends Node<K,V> {
    final Node<K,V>[] nextTable;
    ForwardingNode(Node<K,V>[] tab) {
        super(MOVED, null, null, null);
        this.nextTable = tab;
    }

    Node<K,V> find(int h, Object k) {
        // loop to avoid arbitrarily deep recursion on forwarding nodes
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


    /**
     * 扩容戳
     * @param n
     * @return
     */
    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    
    //这几个方法是原子方法，用它实现并发下的线程安全操作。
    @SuppressWarnings("unchecked")
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }

    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }

    static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }
    
    
