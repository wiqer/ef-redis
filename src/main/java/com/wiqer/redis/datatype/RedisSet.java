package com.wiqer.redis.datatype;

import java.util.*;

/**
 * @author lilan
 */
public class RedisSet implements RedisData
{
    private long timeout = -1;

    private final HashMap<BytesWrapper,BytesWrapper> set = new HashMap<>();


    public RedisSet() {
    }

    @Override
    public long timeout()
    {
        return timeout;
    }

    @Override
    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    public int sadd(List<BytesWrapper> members)
    {
        return (int) members.stream().filter(key -> {
            BytesWrapper oldValve =  set.put(key,key);
            if(oldValve == null){
                return true;
            }
            if(oldValve.equals(key)){
                oldValve.recovery();
                return true;
            }
            return false;
        }).count();
    }

    public Collection<BytesWrapper> keys()
    {
        return set.keySet();
    }

    public int srem(List<BytesWrapper> members)
    {
        return (int) members.stream().filter(key -> {
            BytesWrapper oldValve = set.remove(key);
            if(oldValve != null){
                oldValve.recovery();
                return true;
            }
            return false;
        } ).count();
    }

    @Override
    public void clear() {
        set.clear();
        timeout = -1;
    }

}
