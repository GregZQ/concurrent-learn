package com.conclearn;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author GregZQ
 * @create 2019-02-07 10:11
 * @des: 通过cas实现非阻塞队列
 */
class NonBlockingQueue<T>{
    private Node<T> dummy = new Node<>(null,null);
    private AtomicReference<Node<T>> head = new AtomicReference<>(dummy);
    private AtomicReference<Node<T>> tail = new AtomicReference<>(dummy);

    public void push(T t){
        Node<T> oldValue;
        Node<T> nextValue;
        Node<T> newValue = new Node<>(t,null);
        while(true){
            //获取最后一个节点以及它的next
            oldValue = tail.get();
            nextValue = oldValue.next.get();
            //哦按段是不是最后一个
            if (tail.get() == oldValue){
                //如果是的话判断它的尾节点是不是空（因为这个时候可能其他的节点会进行插入操作）
                //如果不是空协助设置新的尾节点
                if (nextValue != null){
                    tail.compareAndSet(oldValue,nextValue);
                }
            }else{
                //否则的话当前节点就是尾节点，设置值
                if (tail.get().next.compareAndSet(null,newValue)){
                    tail.compareAndSet(oldValue,newValue);
                    return;
                }
            }
        }
    }

    static class Node<T>{
        T current;
        AtomicReference<Node<T>> next;
        public Node(T current,Node<T> next){
            this.current =current;
            this.next =new AtomicReference<>(next);
        }
    }
}
public class NonBlockingQueueUse {

}
