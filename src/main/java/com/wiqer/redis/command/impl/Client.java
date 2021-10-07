package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.SimpleString;
import com.wiqer.redis.util.TRACEID;
import io.netty.channel.ChannelHandlerContext;

public class Client implements Command
{
    private String subCommand;
    private Resp[] array;

    @Override
    public CommandType type()
    {
        return CommandType.client;
    }

    @Override
    public void setContent(Resp[] array)
    {
        this.array = array;
        subCommand = ((BulkString) array[1]).getContent().toUtf8String();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        String traceId = TRACEID.currentTraceId();
        LOGGER.debug("traceId:{} 当前的子命令是：{}", traceId, subCommand);
        switch (subCommand)
        {
            case "setname":
                BytesWrapper connectionName = ((BulkString) array[2]).getContent();
                redisCore.putClient(connectionName, ctx.channel());
                break;
            default:
                throw new IllegalArgumentException();
        }
        ctx.writeAndFlush(new SimpleString("OK"));
    }
}
