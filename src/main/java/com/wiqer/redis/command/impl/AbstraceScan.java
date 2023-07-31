package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.resp.*;
import io.netty.channel.ChannelHandlerContext;

import java.util.Arrays;

public abstract class AbstraceScan implements Command
{

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        final Resp[]     array       = new Resp[2];
        final BulkString bulkString =  RedisBaseData.getRedisDataByType(BulkString.class);
        bulkString.setContent(BytesWrapper.ZERO);
        array[0] = bulkString;
        array[1] = get(redisCore);
        final BytesWrapper key = getKey(redisCore);
        final RespArray i = RedisBaseData.getRedisDataByType(RespArray.class);
        i.setArray(array);
        ctx.writeAndFlush(i).addListener(future -> {
            i.recovery();
            key.recovery();
            Arrays.stream(((RespArray)array[1]).getArray()).forEach(Resp::recovery);
            array[1].recovery();
            array[0].recovery();
        });

    }

    protected abstract RespArray get(RedisCore redisCore);

    protected abstract BytesWrapper getKey(RedisCore redisCore);
}
