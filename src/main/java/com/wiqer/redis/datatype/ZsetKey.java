package com.wiqer.redis.datatype;

public class ZsetKey implements RedisBaseData
{
    BytesWrapper key;
    long         score;

    public ZsetKey(BytesWrapper key, long score)
    {
        this.key = key;
        this.score = score;
    }

    public  ZsetKey()
    {
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

    @Override
    public void clear() {
    }
}
