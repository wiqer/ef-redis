package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.*;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.SimpleString;
import io.netty.channel.ChannelHandlerContext;

public class Type implements Command
{
    private BytesWrapper key;

    @Override
    public CommandType type()
    {
        return CommandType.type;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            SimpleString none =  RedisBaseData.getRedisDataByType(SimpleString.class);
            none.setContent("none");
            ctx.writeAndFlush(none);
        }
        else if (redisData instanceof RedisString)
        {
            SimpleString string =  RedisBaseData.getRedisDataByType(SimpleString.class);
            string.setContent("string");
            ctx.writeAndFlush(string);
        }
        else if (redisData instanceof RedisList)
        {
            SimpleString list =  RedisBaseData.getRedisDataByType(SimpleString.class);
            list.setContent("list");
            ctx.writeAndFlush(list);
        }
        else if (redisData instanceof RedisSet)
        {
            SimpleString set =  RedisBaseData.getRedisDataByType(SimpleString.class);
            set.setContent("set");
            ctx.writeAndFlush(set);
        }
        else if (redisData instanceof RedisHash)
        {
            SimpleString hash =  RedisBaseData.getRedisDataByType(SimpleString.class);
            hash.setContent("hash");
            ctx.writeAndFlush(hash);
        }
        else if (redisData instanceof RedisZset)
        {
            SimpleString zset =  RedisBaseData.getRedisDataByType(SimpleString.class);
            zset.setContent("zset");
            ctx.writeAndFlush(zset);
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
