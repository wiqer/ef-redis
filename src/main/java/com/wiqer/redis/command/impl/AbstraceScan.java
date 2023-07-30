package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.resp.*;
import io.netty.channel.ChannelHandlerContext;

public abstract class AbstraceScan implements Command
{

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        Resp[]     array       = new Resp[2];
        BulkString bulkString =  RedisBaseData.getRedisDataByType(BulkString.class);
        bulkString.setContent(BytesWrapper.ZERO);
        array[0] = bulkString;
        array[1] = get(redisCore);
        RespArray i = RedisBaseData.getRedisDataByType(RespArray.class);
        i.setArray(array);
        ctx.writeAndFlush(i);
    }

    protected abstract RespArray get(RedisCore redisCore);
}
