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
            ctx.writeAndFlush(new SimpleString("none"));
        }
        else if (redisData instanceof RedisString)
        {
            ctx.writeAndFlush(new SimpleString("string"));
        }
        else if (redisData instanceof RedisList)
        {
            ctx.writeAndFlush(new SimpleString("list"));
        }
        else if (redisData instanceof RedisSet)
        {
            ctx.writeAndFlush(new SimpleString("set"));
        }
        else if (redisData instanceof RedisHash)
        {
            ctx.writeAndFlush(new SimpleString("hash"));
        }
        else if (redisData instanceof RedisZset)
        {
            ctx.writeAndFlush(new SimpleString("zset"));
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
