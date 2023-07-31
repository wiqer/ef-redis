package com.wiqer.redis.command.impl;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisList;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import com.wiqer.redis.resp.SimpleString;
import io.netty.channel.ChannelHandlerContext;

public class Ping implements Command
{

    @Override
    public CommandType type()
    {
        return CommandType.lrem;
    }

    @Override
    public void setContent(Resp[] array)
    {
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        ctx.write(new SimpleString("PONG"));
        ctx.flush();
    }
}
