package com.wiqer.redis.command.impl.hash;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.impl.AbstraceScan;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisHash;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Hscan extends AbstraceScan
{
    private BytesWrapper key;

    @Override
    public CommandType type()
    {
        return CommandType.hscan;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    protected RespArray get(RedisCore redisCore)
    {
        RedisHash                       redisHash = (RedisHash) redisCore.get(key);
        Map<BytesWrapper, BytesWrapper> map       = redisHash.getMap();
        return new RespArray(map.entrySet().stream().flatMap(entry -> {
            Resp[] resps = new Resp[2];
            BulkString bulkString0 =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkString0.setContent(entry.getKey());
            resps[0] = bulkString0;
            BulkString bulkString1 =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkString1.setContent(entry.getValue());
            resps[1] = bulkString1;
            return Stream.of(resps);
        }).toArray(Resp[]::new));
    }

    @Override
    protected BytesWrapper getKey(RedisCore redisCore) {
        return key;
    }
}
