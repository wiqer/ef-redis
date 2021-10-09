package com.wiqer.redis.aof;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程不安全的BlockingQueue，尽量单线程使用，尽量使用 offer和poll两个底层方法
 * @param <E> 泛型
 */
public class RingBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 29;

    //一块连续内存页肯定能装下
    static final int MAXIMUM_SUBAREA = 1 << 12;

    Object data[][];

    volatile int   readIndex=-1;

    volatile int   writeIndex=-1;

    private final AtomicInteger count = new AtomicInteger();

    /** Lock held by take, poll, etc */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** Wait queue for waiting takes */
    private final Condition notEmpty = takeLock.newCondition();

    /** Lock held by put, offer, etc */
    private final ReentrantLock putLock = new ReentrantLock();

    /** Wait queue for waiting puts */
    private final Condition notFull = putLock.newCondition();

    /**
     * Signals a waiting take. Called only from put/offer (which do not
     * otherwise ordinarily lock takeLock.)
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * Signals a waiting put. Called only from take/poll.
     */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    int capacity;

    int rowOffice;

    int colOffice;

    int rowSize;

    int bitHigh;

    int subareaSize;

    int maxSize;

    /**
     * 初始化队列
     * @param subareaSize 分片数
     * @param capacity 容量
     */
    public RingBlockingQueue(int subareaSize, int capacity)  {
        this(subareaSize, capacity,1);
    }

    /**
     * 初始化队列
     * @param subareaSize 分片数
     * @param capacity 容量
     * @param concurrency 并发数
     */
    public RingBlockingQueue(int subareaSize, int capacity, int concurrency)  {

       if(subareaSize>capacity||capacity<0||subareaSize<0){
           throw new IllegalArgumentException("Illegal initial capacity:subareaSize>capacity||capacity<0||subareaSize<0");
       }
        maxSize=capacity;
        subareaSize =subareaSizeFor(subareaSize);
        capacity=tableSizeFor(capacity);
         rowSize= tableSizeFor(capacity/subareaSize);
        capacity=rowSize*subareaSize;

        data=new Object[rowSize][subareaSize];
        this.capacity=capacity;
        bitHigh=getIntHigh(subareaSize);
        this.subareaSize=subareaSize;
        rowOffice=rowSize-1;
        colOffice=subareaSize-1;

    }

    /**
     * 初始化队列
     * @param c 集合
     */
    public RingBlockingQueue(Collection<? extends E> c) {
        this(8888,88888);
        final ReentrantLock putLock = this.putLock;
        putLock.lock(); // Never contended, but necessary for visibility
        try {
            int n = 0;
            for (E e : c) {
                if (e == null) {
                    throw new NullPointerException();
                }
                if (n == capacity) {
                    throw new IllegalStateException("Queue full");
                }
                put(e);
                ++n;
            }
            count.set(n);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            putLock.unlock();
        }
    }

    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
    static final int getIntHigh(int cap){
        int high=0;
        while ((cap&1)==0){
            high++;
            cap=cap>>1;
        }
        return high;
    }
    static final int subareaSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_SUBAREA) ? MAXIMUM_SUBAREA : n + 1;
    }
    void refreshIndex(){
       if( readIndex>capacity){
           putLock.lock();
           try {
               synchronized (this) {
                   if (readIndex > capacity) {
                       writeIndex -= capacity;
                       readIndex -= capacity;
                   }
               }
           }finally {
               putLock.unlock();
           }

       }


    }
    @Override
    public boolean offer(Object o) {
        int localWriteIndex=0;
        synchronized (this) {
            if(writeIndex>readIndex+maxSize) {
                return  false;
            }
            count.incrementAndGet();
            localWriteIndex= ++writeIndex;
        }
        int row=(localWriteIndex>>bitHigh)&(rowOffice);
        int column =localWriteIndex&(colOffice);
        if(column==0&&row==0){
            refreshIndex();
        }
        data[row][column]=o;
        return true;
    }



    @Override
    public E poll() {
        int localReadIndex=0;
        synchronized (this){
            if(writeIndex>readIndex){
                localReadIndex=++readIndex;
                count.getAndDecrement();
            }else{
                return null;
            }
        }
        int row=(localReadIndex>>bitHigh)&(rowOffice);
        int column =localReadIndex&(colOffice);
        if(column==0&&row==0){
            refreshIndex();
        }
        return (E)data[row][column];
    }
    E ergodic(Integer index){
        int localReadIndex=0;
        if(index>writeIndex||index < readIndex){
            return null;
        }
        int row=(index>>bitHigh)&(rowOffice);
        int column =index&(colOffice);
        if(column==0&&row==0){
            refreshIndex();
        }
        return (E)data[row][column];
    }



    @Override
    public E peek() {
        return null;
    }

    @Override
    public void put(E o) throws InterruptedException {
        if (o == null) {
            throw new NullPointerException();
        }
        // Note: convention in all put/take/etc is to preset local var
        // holding count negative to indicate failure unless set.
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            /*
             * Note that count is used in wait guard even though it is
             * not protected by lock. This works because count can
             * only decrease at this point (all other puts are shut
             * out by lock), and we (or some other waiting put) are
             * signalled if it ever changes from capacity. Similarly
             * for all other uses of count in other wait guards.
             */
            while (count.get() == capacity) {
                notFull.await();
            }
            offer(o);
            c = count.getAndIncrement();
            if (c + 1 < capacity) {
                notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0) {
            signalNotEmpty();
        }
    }

    @Override
    public boolean offer(Object o, long timeout, TimeUnit unit) throws InterruptedException {
        if (o == null) {
            throw new NullPointerException();
        }
        long nanos = unit.toNanos(timeout);
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            while (count.get() == capacity) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            offer(o);
            c = count.getAndIncrement();
            if (c + 1 < capacity) {
                notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0) {
            signalNotEmpty();
        }
        return true;
    }

    @Override
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = poll();
            c = count.getAndDecrement();
            if (c > 1) {
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity) {
            signalNotFull();
        }
        return x;

    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x = null;
        int c = -1;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            x = poll();
            c = count.getAndDecrement();
            if (c > 1) {
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity) {
            signalNotFull();
        }
        return x;
    }

    @Override
    public int remainingCapacity() {
        return capacity - count.get();
    }

    /**
     * 没有实现此方法
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o) {
        return false;
    }


    /**
     * 没有实现此方法
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        return false;
    }
    /**
     * 没有实现此方法
     * @return
     */
    @Override
    public int hashCode() {
        return Arrays.deepHashCode(data);
    }
    /**
     * 没有实现此方法
     * @param c
     * @return
     */
    @Override
    public boolean retainAll(Collection c) {
        return false;
    }
    /**
     * 没有实现此方法
     * @param c
     * @return
     */
    @Override
    public boolean removeAll(Collection c) {
        return false;
    }

    /**
     * 没有实现此方法
     * @param c
     * @return
     */
    @Override
    public boolean containsAll(Collection c) {
        return false;
    }

    @Override
    public int size() {
        return count.get();
    }

    @Override
    public boolean isEmpty() {
        return count.get()==0;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        fullyLock();
        try {
            for (int index = readIndex; readIndex>=index||index<=writeIndex; index++) {
                if (o.equals(ergodic(index))) {
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 没有实现删除方法，没有实现迭代器
     * @return
     */
    @Override
    public Iterator<E> iterator() {
        return null;
    }

    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }

    /**
     * Unlocks to allow both puts and takes.
     */
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    @Override
    public Object[] toArray() {
        fullyLock();
        try {
            int size = count.get();
            Object[] a = new Object[size];
            int k = 0;
            for (int index = readIndex; readIndex>=index||index<=writeIndex; index++) {
                a[k++] = ergodic(index);
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public <E> E[] toArray(E[] a){
        fullyLock();
        try {
            int size = count.get();
            if (a.length < size) {
                a = (E[])java.lang.reflect.Array.newInstance
                        (a.getClass().getComponentType(), size);
            }

            int k = 0;
            for (int index = readIndex; readIndex>=index||index<=writeIndex; index++) {
                a[k++] = (E)ergodic(index);
            }
            if (a.length > k) {
                a[k] = null;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }


    @Override
    public int drainTo(Collection c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        boolean signalNotFull = false;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            int n = Math.min(maxElements, count.get());
            // count.get provides visibility to first n Nodes
            E e;
            int i = 0;
            try {
                while ((e=remove())!=null) {

                    c.add(e);

                }
                return n;
            } finally {
                // Restore invariants even if c.add() threw
                if (i > 0) {
                    signalNotFull = (count.getAndAdd(-i) == capacity);
                }
            }
        } finally {
            takeLock.unlock();
            if (signalNotFull) {
                signalNotFull();
            }
        }
    }
    @Override
    public boolean add(E e) {
        if (offer(e)) {
            return true;
        } else {
            throw new IllegalStateException("Queue full");
        }
    }

    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from {@link #poll poll} only in that it throws an exception if this
     * queue is empty.
     *
     * <p>This implementation returns the result of <tt>poll</tt>
     * unless the queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public E remove() {
        E x = poll();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Retrieves, but does not remove, the head of this queue.  This method
     * differs from {@link #peek peek} only in that it throws an exception if
     * this queue is empty.
     *
     * <p>This implementation returns the result of <tt>peek</tt>
     * unless the queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public E element() {
        E x = peek();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     *
     * <p>This implementation repeatedly invokes {@link #poll poll} until it
     * returns <tt>null</tt>.
     */
    @Override
    public void clear() {
        while (poll() != null) {
            ;
        }
    }

    /**
     * Adds all of the elements in the specified collection to this
     * queue.  Attempts to addAll of a queue to itself result in
     * <tt>IllegalArgumentException</tt>. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * <p>This implementation iterates over the specified collection,
     * and adds each element returned by the iterator to this
     * queue, in turn.  A runtime exception encountered while
     * trying to add an element (including, in particular, a
     * <tt>null</tt> element) may result in only some of the elements
     * having been successfully added when the associated exception is
     * thrown.
     *
     * @param c collection containing elements to be added to this queue
     * @return <tt>true</tt> if this queue changed as a result of the call
     * @throws ClassCastException if the class of an element of the specified
     *         collection prevents it from being added to this queue
     * @throws NullPointerException if the specified collection contains a
     *         null element and this queue does not permit null elements,
     *         or if the specified collection is null
     * @throws IllegalArgumentException if some property of an element of the
     *         specified collection prevents it from being added to this
     *         queue, or if the specified collection is this queue
     * @throws IllegalStateException if not all the elements can be added at
     *         this time due to insertion restrictions
     * @see #add(Object)
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }
}
