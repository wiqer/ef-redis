package com.wiqer.redis.command.impl.set;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisSet;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Sadd implements WriteCommand
{
    List<BytesWrapper> member;
    private BytesWrapper key;

    @Override
    public CommandType type()
    {
        return CommandType.sadd;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        member = Stream.of(array).skip(2).map(resp -> ((BulkString) resp).getContent()).collect(Collectors.toList());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            RedisSet redisSet =  RedisBaseData.getRedisDataByType(RedisSet.class);
            int      sadd     = redisSet.sadd(member);
            redisCore.put(key, redisSet);
            RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
            i.setValue(sadd);
            ctx.writeAndFlush(i);
            i.recovery();
        }
        else if (redisData instanceof RedisSet)
        {
            RedisSet redisSet = (RedisSet) redisData;
            int      sadd     = redisSet.sadd(member);
            RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
            i.setValue(sadd);
            ctx.writeAndFlush(i);
            i.recovery();
        }
        else
        {
            throw new IllegalArgumentException("类型不匹配");
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            RedisSet redisSet =  RedisBaseData.getRedisDataByType(RedisSet.class);
            redisSet.sadd(member);
            redisCore.put(key, redisSet);
        }
        else if (redisData instanceof RedisSet)
        {
            RedisSet redisSet = (RedisSet) redisData;
            redisSet.sadd(member);
        }
        else
        {
            throw new IllegalArgumentException("类型不匹配");
        }
    }
}
