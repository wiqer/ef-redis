package com.wiqer.redis;


import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import io.netty.channel.Channel;

import java.util.List;
import java.util.Set;

public interface RedisCore {
    Set<BytesWrapper> keys();

    void putClient(BytesWrapper connectionName, Channel channelContext);

    boolean exist(BytesWrapper key);

    void put(BytesWrapper key, RedisData redisData);

    RedisData get(BytesWrapper key);

    long remove(List<BytesWrapper> keys);

    void cleanAll();

    void startTllTask(int hz,long maxMemory);
}
