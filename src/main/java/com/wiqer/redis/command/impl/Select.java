package com.wiqer.redis.command.impl;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.SimpleString;
import io.netty.channel.ChannelHandlerContext;

public class Select implements Command
{
    private Integer index;
    @Override
    public CommandType type()
    {
        return CommandType.select;
    }

    @Override
    public void setContent(Resp[] array)
    {
         index = Integer.parseInt(((BulkString) array[1]).getContent().toUtf8String());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        if(index>0){
            SimpleString ok = new SimpleString("-ERR invalid DB index");
            ctx.writeAndFlush(ok);
        }else {
            SimpleString ok = new SimpleString("OK");
            ctx.writeAndFlush(ok);
        }

    }
}