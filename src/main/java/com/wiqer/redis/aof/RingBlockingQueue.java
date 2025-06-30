package com.wiqer.redis.aof;

import io.github.karlatemp.unsafeaccessor.Unsafe;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 环形阻塞队列实现
 * 支持单线程读单线程写并发场景
 *
 * @param <E> 泛型
 */
public class RingBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 29;

    /**
     * 一块连续内存页肯定能装下
     * boolean pa = VM.isDirectMemoryPageAligned();
     * int ps = Bits.pageSize();
     * jdk 11的 jdk.internal.misc.Unsafe.pageSize()
     * jdk 8 的 Unsafe.getUnsafe().pageSize()
     * win 平台一般是 1 << 12 也就是 4096
     */
    static final int MAXIMUM_SUBAREA = Unsafe.getUnsafe().pageSize();

    // 数据存储数组
    private final Object[][] data;

    // 读写索引，使用volatile保证可见性
    private volatile long readIndex = -1;
    private volatile long writeIndex = -1;

    // 元素计数
    private final AtomicInteger count = new AtomicInteger();

    // 主锁，保护所有核心操作
    private final ReentrantLock mainLock = new ReentrantLock();

    // 等待队列
    private final Condition notEmpty = mainLock.newCondition();
    private final Condition notFull = mainLock.newCondition();

    // 队列配置参数
    private final int capacity;
    private final int rowOffice;
    private final int colOffice;
    private final int rowSize;
    private final int bitHigh;
    private final int maxSize;

    /**
     * 初始化队列
     *
     * @param subareaSize 分片数
     * @param capacity    容量
     */
    public RingBlockingQueue(int subareaSize, int capacity) {
        this(subareaSize, capacity, 1);
    }

    /**
     * 初始化队列
     *
     * @param subareaSize 分片数
     * @param capacity    容量
     * @param concurrency 并发数（已废弃，保留兼容性）
     */
    public RingBlockingQueue(int subareaSize, int capacity, int concurrency) {
        if (subareaSize > capacity || capacity < 0 || subareaSize < 0) {
            throw new IllegalArgumentException("Illegal initial capacity:subareaSize>capacity||capacity<0||subareaSize<0");
        }
        
        this.maxSize = capacity;
        subareaSize = subareaSizeFor(subareaSize);
        capacity = tableSizeFor(capacity);
        rowSize = tableSizeFor(capacity / subareaSize);
        capacity = rowSize * subareaSize;

        this.data = new Object[rowSize][subareaSize];
        this.capacity = capacity;
        this.bitHigh = getIntHigh(subareaSize);
        this.rowOffice = rowSize - 1;
        this.colOffice = subareaSize - 1;
    }

    /**
     * 初始化队列
     *
     * @param c 集合
     */
    public RingBlockingQueue(Collection<? extends E> c) {
        this(8888, 88888);
        mainLock.lock();
        try {
            int n = 0;
            for (E e : c) {
                if (e == null) {
                    throw new NullPointerException();
                }
                if (n == capacity) {
                    throw new IllegalStateException("Queue full");
                }
                internalOffer(e);
                ++n;
            }
            count.set(n);
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 计算2的幂次方容量
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * 计算整数的最高位
     */
    static final int getIntHigh(int cap) {
        int high = 0;
        while ((cap & 1) == 0) {
            high++;
            cap = cap >> 1;
        }
        return high;
    }

    /**
     * 计算分片大小的2的幂次方
     */
    static final int subareaSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_SUBAREA) ? MAXIMUM_SUBAREA : n + 1;
    }

    /**
     * 刷新索引，防止索引溢出
     */
    private void refreshIndex() {
        if (readIndex > capacity) {
            writeIndex -= capacity;
            readIndex -= capacity;
        }
    }

    /**
     * 内部offer方法，不包含锁操作
     */
    private boolean internalOffer(E o) {
        if (writeIndex > readIndex + maxSize) {
            return false;
        }
        
        long localWriteIndex = ++writeIndex;
        int row = (int) ((localWriteIndex >> bitHigh) & rowOffice);
        int column = (int) (localWriteIndex & colOffice);
        
        if (column == 0 && row == 0) {
            refreshIndex();
        }
        
        data[row][column] = o;
        return true;
    }

    /**
     * 内部poll方法，不包含锁操作
     */
    private E internalPoll() {
        if (writeIndex <= readIndex) {
            return null;
        }
        
        long localReadIndex = ++readIndex;
        int row = (int) ((localReadIndex >> bitHigh) & rowOffice);
        int column = (int) (localReadIndex & colOffice);
        
        if (column == 0 && row == 0) {
            refreshIndex();
        }
        
        @SuppressWarnings("unchecked")
        E result = (E) data[row][column];
        data[row][column] = null; // 清除引用，帮助GC
        return result;
    }

    /**
     * 内部peek方法，不包含锁操作
     */
    private E internalPeek() {
        if (writeIndex <= readIndex) {
            return null;
        }
        
        long localReadIndex = readIndex + 1;
        int row = (int) ((localReadIndex >> bitHigh) & rowOffice);
        int column = (int) (localReadIndex & colOffice);
        
        if (column == 0 && row == 0) {
            refreshIndex();
        }
        
        @SuppressWarnings("unchecked")
        E result = (E) data[row][column];
        return result;
    }

    @Override
    public boolean offer(E o) {
        if (o == null) {
            throw new NullPointerException();
        }
        
        mainLock.lock();
        try {
            if (count.get() >= capacity) {
                return false;
            }
            
            if (internalOffer(o)) {
                count.incrementAndGet();
                notEmpty.signal();
                return true;
            }
            return false;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public E poll() {
        mainLock.lock();
        try {
            if (count.get() == 0) {
                return null;
            }
            
            E result = internalPoll();
            if (result != null) {
                count.decrementAndGet();
                notFull.signal();
            }
            return result;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public E peek() {
        mainLock.lock();
        try {
            return internalPeek();
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void put(E o) throws InterruptedException {
        if (o == null) {
            throw new NullPointerException();
        }
        
        mainLock.lockInterruptibly();
        try {
            while (count.get() >= capacity) {
                notFull.await();
            }
            
            internalOffer(o);
            count.incrementAndGet();
            notEmpty.signal();
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
        if (o == null) {
            throw new NullPointerException();
        }
        
        long nanos = unit.toNanos(timeout);
        mainLock.lockInterruptibly();
        try {
            while (count.get() >= capacity) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            
            internalOffer(o);
            count.incrementAndGet();
            notEmpty.signal();
            return true;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        mainLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            
            E result = internalPoll();
            count.decrementAndGet();
            notFull.signal();
            return result;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        mainLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            
            E result = internalPoll();
            count.decrementAndGet();
            notFull.signal();
            return result;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        mainLock.lock();
        try {
            return capacity - count.get();
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        
        mainLock.lock();
        try {
            // 简单实现：遍历查找并移除
            for (int i = 0; i < count.get(); i++) {
                long index = readIndex + 1 + i;
                int row = (int) ((index >> bitHigh) & rowOffice);
                int column = (int) (index & colOffice);
                
                if (o.equals(data[row][column])) {
                    // 找到元素，移除它
                    data[row][column] = null;
                    count.decrementAndGet();
                    return true;
                }
            }
            return false;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        
        mainLock.lock();
        try {
            for (int i = 0; i < count.get(); i++) {
                long index = readIndex + 1 + i;
                int row = (int) ((index >> bitHigh) & rowOffice);
                int column = (int) (index & colOffice);
                
                if (o.equals(data[row][column])) {
                    return true;
                }
            }
            return false;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public int size() {
        return count.get();
    }

    @Override
    public boolean isEmpty() {
        return count.get() == 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new RingBlockingQueueIterator();
    }

    /**
     * 迭代器实现
     */
    private class RingBlockingQueueIterator implements Iterator<E> {
        private int currentIndex = 0;
        private final int size = count.get();
        private final long startReadIndex = readIndex;

        @Override
        public boolean hasNext() {
            return currentIndex < size;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            mainLock.lock();
            try {
                long index = startReadIndex + 1 + currentIndex;
                int row = (int) ((index >> bitHigh) & rowOffice);
                int column = (int) (index & colOffice);
                
                @SuppressWarnings("unchecked")
                E result = (E) data[row][column];
                currentIndex++;
                return result;
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Object[] toArray() {
        mainLock.lock();
        try {
            int size = count.get();
            Object[] a = new Object[size];
            for (int i = 0; i < size; i++) {
                long index = readIndex + 1 + i;
                int row = (int) ((index >> bitHigh) & rowOffice);
                int column = (int) (index & colOffice);
                a[i] = data[row][column];
            }
            return a;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        mainLock.lock();
        try {
            int size = count.get();
            if (a.length < size) {
                a = (T[]) java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);
            }

            for (int i = 0; i < size; i++) {
                long index = readIndex + 1 + i;
                int row = (int) ((index >> bitHigh) & rowOffice);
                int column = (int) (index & colOffice);
                a[i] = (T) data[row][column];
            }
            
            if (a.length > size) {
                a[size] = null;
            }
            return a;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        
        mainLock.lock();
        try {
            int n = Math.min(maxElements, count.get());
            for (int i = 0; i < n; i++) {
                E e = internalPoll();
                if (e != null) {
                    c.add(e);
                }
            }
            count.addAndGet(-n);
            notFull.signal();
            return n;
        } finally {
            mainLock.unlock();
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

    @Override
    public E remove() {
        E x = poll();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E element() {
        E x = peek();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void clear() {
        mainLock.lock();
        try {
            while (count.get() > 0) {
                internalPoll();
                count.decrementAndGet();
            }
            notFull.signal();
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        
        mainLock.lock();
        try {
            boolean modified = false;
            for (E e : c) {
                if (e == null) {
                    throw new NullPointerException();
                }
                if (count.get() < capacity) {
                    internalOffer(e);
                    count.incrementAndGet();
                    modified = true;
                } else {
                    throw new IllegalStateException("Queue full");
                }
            }
            if (modified) {
                notEmpty.signal();
            }
            return modified;
        } finally {
            mainLock.unlock();
        }
    }

    // 兼容性方法，返回false
    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(data);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }
}
