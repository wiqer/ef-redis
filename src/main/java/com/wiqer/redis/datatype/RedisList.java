package com.wiqer.redis.datatype;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lilan
 */
public class RedisList implements RedisData {
    private long timeout = -1;
    private Deque<BytesWrapper> deque = new LinkedList<>();

    public RedisList() {
    }

    @Override
    public long timeout() {
        return timeout;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void lpush(BytesWrapper... values) {
        for (BytesWrapper value : values) {
            deque.addFirst(value);
        }
    }

    public int size() {
        return deque.size();
    }

    public void lpush(List<BytesWrapper> values) {
        for (BytesWrapper value : values) {
            deque.addFirst(value);
        }
    }

    public void rpush(List<BytesWrapper> values) {
        for (BytesWrapper value : values) {
            deque.addLast(value);
        }
    }

    public List<BytesWrapper> lrang(int start, int end) {
        return deque.stream().skip(start).limit(end - start >= 0 ? end - start + 1 : 0).collect(Collectors.toList());
    }

    public int remove(BytesWrapper value) {
        int count = 0;
        while (deque.remove(value)) {
            count++;
        }
        value.recovery();
        return count;
    }

    @Override
    public void clear() {
        deque = new LinkedList<>();
        timeout = -1;
    }
}
