package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import com.wiqer.redis.resp.SimpleString;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Del implements WriteCommand
{
    private List<BytesWrapper> keys;

    @Override
    public CommandType type()
    {
        return CommandType.del;
    }

    @Override
    public void setContent(Resp[] array)
    {
        keys = Stream.of(array).skip(1).map(resp -> ((BulkString) resp).getContent()).collect(Collectors.toList());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        long remove = redisCore.remove(keys);
        RespInt ri = RedisBaseData.getRedisDataByType(RespInt.class);
        ri.getValue((int) remove);
        ctx.writeAndFlush(ri);
        keys.forEach(BytesWrapper::recovery);
        ri.recovery();

    }

    @Override
    public void handle(RedisCore redisCore) {
        redisCore.remove(keys);
    }
}
