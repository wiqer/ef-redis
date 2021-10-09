package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisZset;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Zrem implements WriteCommand
{
    private BytesWrapper       key;
    private List<BytesWrapper> members;

    @Override
    public CommandType type()
    {
        return CommandType.zrem;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        members = Stream.of(array).skip(2).map(resp -> ((BulkString) resp).getContent()).collect(Collectors.toList());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisZset redisZset = (RedisZset) redisCore.get(key);
        int       remove    = redisZset.remove(members);
        ctx.writeAndFlush(new RespInt(remove));
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisZset redisZset = (RedisZset) redisCore.get(key);
        int       remove    = redisZset.remove(members);
    }
}
