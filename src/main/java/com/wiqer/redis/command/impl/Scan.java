package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;

public class Scan implements Command
{
    @Override
    public CommandType type()
    {
        return CommandType.scan;
    }

    @Override
    public void setContent(Resp[] array)
    {
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        Resp[]     array       = new Resp[2];
        BulkString blukStrings = new BulkString(new BytesWrapper("0".getBytes(CHARSET)));
        array[0] = blukStrings;
        List<BulkString> collect = redisCore.keys().stream().map(keyName -> new BulkString(keyName)).collect(Collectors.toList());
        array[1] = new RespArray(collect.toArray(new Resp[collect.size()]));
        ctx.writeAndFlush(new RespArray(array));
    }
}
