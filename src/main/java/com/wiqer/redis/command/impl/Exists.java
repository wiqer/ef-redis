package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class Exists implements Command
{
    private BytesWrapper key;

    @Override
    public CommandType type()
    {
        return CommandType.exists;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        boolean exist = redisCore.exist(key);
        if (exist)
        {
            ctx.writeAndFlush(new RespInt(1));
        }
        else
        {
            ctx.writeAndFlush(new RespInt(0));
        }
    }
}
