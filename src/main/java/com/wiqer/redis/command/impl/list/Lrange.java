package com.wiqer.redis.command.impl.list;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisList;
import com.wiqer.redis.resp.*;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;

public class Lrange implements Command
{
    BytesWrapper key;
    int          start;
    int          end;

    @Override
    public CommandType type()
    {
        return CommandType.lrange;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        start = Integer.parseInt(((BulkString) array[2]).getContent().toUtf8String());
        end = Integer.parseInt(((BulkString) array[3]).getContent().toUtf8String());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisList          redisList = (RedisList) redisCore.get(key);
        if(redisList == null){
            ctx.writeAndFlush(new Errors("1"));
        }else {
            List<BytesWrapper> lrang     = redisList.lrang(start, end);
            RespArray          respArray = new RespArray(lrang.stream().map(BulkString::new).toArray(Resp[]::new));
            ctx.writeAndFlush(respArray);
        }

    }
}
