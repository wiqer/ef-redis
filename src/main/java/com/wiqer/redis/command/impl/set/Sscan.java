package com.wiqer.redis.command.impl.set;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.impl.AbstraceScan;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisSet;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;

import java.util.List;
import java.util.stream.Collectors;

public class Sscan extends AbstraceScan
{
    private BytesWrapper key;

    @Override
    public CommandType type()
    {
        return CommandType.sscan;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    protected RespArray get(RedisCore redisCore)
    {
        RedisSet         redisSet = (RedisSet) redisCore.get(key);
        List<BulkString> collect  = redisSet.keys().stream().map(keyName -> {
            BulkString bulkStringSub =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkStringSub.setContent(keyName);
            return bulkStringSub;
        }).collect(Collectors.toList());
        RespArray arrays = RedisBaseData.getRedisDataByType(RespArray.class);
        arrays.setArray(collect.toArray(new Resp[collect.size()]));
        return arrays;
    }

    @Override
    protected BytesWrapper getKey(RedisCore redisCore) {
        return key;
    }
}
