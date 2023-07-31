package com.wiqer.redis.datatype;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lilan
 */
public class RedisHash implements RedisData
{
    private       long                            timeout = -1;
    private final Map<BytesWrapper, BytesWrapper> map     = new HashMap<>();

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
        return map.put(field, value) == null ? 1 : 0;
    }

    public Map<BytesWrapper, BytesWrapper> getMap()
    {
        return map;
    }

    public int del(List<BytesWrapper> fields)
    {
        return (int) fields.stream().filter(key -> map.remove(key) != null).count();
    }
}
