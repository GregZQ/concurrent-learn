package com.conclearn;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author GregZQ
 * @create 2019-02-07 9:44
 * @des: 构建非阻塞的栈
 */
class NonBlockingStack<T>{

    private AtomicReference<Node<T>> top = new AtomicReference<>();

    //通过cas插入元素
    public void put(T t){
        Node<T> oldValue;
        Node<T> newValue;
        do{
            oldValue = top.get();
            newValue = new Node<>(t,oldValue);
        }while (top.compareAndSet(oldValue,newValue));
    }
    //通过cas提取元素
    public T pop(){
        Node<T> oldValue;
        Node<T> newValue;
        do{
            oldValue = top.get();
            if (Objects.isNull(oldValue)){
                return null;
            }
            newValue = oldValue.next;
        }while (!top.compareAndSet(oldValue,newValue));
        return oldValue.current;


    }

    private static class Node<T>{
        private T current;
        private Node<T> next;
        public Node(T current,Node<T> next){
            this.current = current;
            this.next = next;
        }
    }
}
public class NonBlockingStackUse {
}
