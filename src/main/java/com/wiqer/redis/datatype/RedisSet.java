package com.wiqer.redis.datatype;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lilan
 */
public class RedisSet implements RedisData {
    private long timeout = -1;

    private final Set<BytesWrapper> set = new HashSet<>();

    @Override
    public long timeout() {
        return timeout;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int sadd(List<BytesWrapper> members) {
        return (int) members.stream().filter(set::add).count();
    }

    public Collection<BytesWrapper> keys() {
        return set;
    }

    public int srem(List<BytesWrapper> members) {
        return (int) members.stream().filter(set::remove).count();
    }

    public Set<BytesWrapper> spop(int n) {
        final int size = set.size();
        n = Math.min(n, size);
        Random random = new Random();
        Set<BytesWrapper> deletedElements = new HashSet<>();
        Set<BytesWrapper> resSet = new HashSet<>();
        while (resSet.size() < n){
            for (BytesWrapper val : set){
                if(n > random.nextInt(size)){
                    resSet.add(val);
                }
                if(resSet.size() >= n){
                    break;
                }
            }
        }
        set.removeAll(resSet);
        return deletedElements;
    }
}
