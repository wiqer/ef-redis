package com.wiqer.redis.datatype;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lilan
 * Hash 垃圾回收key搁置 todo
 */
public class RedisHash implements RedisData
{
    private       long                            timeout = -1;
    private final Map<BytesWrapper, BytesWrapper> map     = new HashMap<>();

    public RedisHash() {
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

    public int put(BytesWrapper field, BytesWrapper value)
    {
        BytesWrapper old = map.put(field, value);
        if(old != null){
            old.recovery();
            return 0;
        }
        return 1;
    }

    public Map<BytesWrapper, BytesWrapper> getMap()
    {
        return map;
    }

    public int del(List<BytesWrapper> fields)
    {
        return (int) fields.stream().filter(key -> {
            BytesWrapper value = map.remove(key);
            if(value!= null){
                value.recovery();
                return true;
            }
            return false;
        } ).count();
    }

    @Override
    public void clear() {
        map.clear();
        timeout = -1;
    }

}
