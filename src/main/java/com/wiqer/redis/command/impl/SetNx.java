package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class SetNx implements Command
{
    private BytesWrapper key;
    private BytesWrapper value;

    @Override
    public CommandType type()
    {
        return CommandType.setnx;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        value = ((BulkString) array[2]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        boolean exist = redisCore.exist(key);
        if (exist)
        {
            ctx.writeAndFlush(new RespInt(0));
        }
        else
        {
            RedisString redisString = new RedisString();
            redisString.setValue(value);
            redisCore.put(key, redisString);
            ctx.writeAndFlush(new RespInt(1));
        }
    }
}
