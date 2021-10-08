package com.wiqer.redis.datatype;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author lilan
 */
public class RedisZset implements RedisData
{
    private long                   timeout = -1;
    private TreeMap<ZsetKey, Long> map     = new TreeMap<>(new Comparator<ZsetKey>()
    {
        @Override
        public int compare(ZsetKey o1, ZsetKey o2)
        {
            if (o1.key.equals(o2.key))
            {
                return 0;
            }
            return Long.compare(o1.score, o2.score);
        }
    });

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

    public int add(List<ZsetKey> keys)
    {
        return (int) keys.stream().peek(key -> {
            map.put(key,key.getScore());
        }).count();
    }

    public List<ZsetKey> range(int start, int end)
    {
        return map.keySet().stream().skip(start).limit(end - start >= 0 ? end - start + 1 : 0).collect(Collectors.toList());
    }

    public List<ZsetKey> reRange(int start, int end)
    {
        return map.descendingKeySet().descendingSet().stream().skip(start).limit(end - start >= 0 ? end - start + 1 : 0).collect(Collectors.toList());
    }

    public int remove(List<BytesWrapper> members)
    {
        return (int) members.stream().filter(member ->map.remove(new ZsetKey(member,0))!=null).count();
    }

    public static class ZsetKey
    {
        BytesWrapper key;
        long         score;

        public ZsetKey(BytesWrapper key, long score)
        {
            this.key = key;
            this.score = score;
        }

        public BytesWrapper getKey()
        {
            return key;
        }

        public long getScore()
        {
            return score;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            ZsetKey zsetKey = (ZsetKey) o;
            return key.equals(zsetKey.key);
        }

        @Override
        public int hashCode()
        {
            return key.hashCode();
        }
    }
}
