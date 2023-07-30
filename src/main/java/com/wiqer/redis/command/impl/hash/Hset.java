package com.wiqer.redis.command.impl.hash;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.*;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class Hset implements WriteCommand
{
    private BytesWrapper key;
    private BytesWrapper field;
    private BytesWrapper value;

    @Override
    public CommandType type()
    {
        return CommandType.hset;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        field = ((BulkString) array[2]).getContent();
        value = ((BulkString) array[3]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            RedisHash redisHash =  RedisBaseData.getRedisDataByType(RedisHash.class);

            int       put       = redisHash.put(field, value);
            redisCore.put(key, redisHash);
            RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
            i.getValue(put);
            ctx.writeAndFlush(i);
        }
        else if (redisData instanceof RedisHash)
        {
            RedisHash redisHash = (RedisHash) redisData;
            int       put       = redisHash.put(field, value);
            RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
            i.getValue(put);
            ctx.writeAndFlush(i);
        }
        else
        {
            throw new IllegalArgumentException("类型错误");
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            RedisHash redisHash =  RedisBaseData.getRedisDataByType(RedisHash.class);
            redisHash.put(field, value);
            redisCore.put(key, redisHash);
        }
        else if (redisData instanceof RedisHash)
        {
            RedisHash redisHash = (RedisHash) redisData;
             redisHash.put(field, value);
        }
        else
        {
            throw new IllegalArgumentException("类型错误");
        }
    }
}
