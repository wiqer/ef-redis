package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.*;
import com.wiqer.redis.resp.*;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class Push implements WriteCommand
{
    BiConsumer<RedisList, List<BytesWrapper>> biConsumer;
    private BytesWrapper       key;
    private List<BytesWrapper> value;

    public Push(BiConsumer<RedisList, List<BytesWrapper>> biConsumer)
    {
        this.biConsumer = biConsumer;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        value = new ArrayList<>();
        for (int i = 2; i < array.length; i++)
        {
            value.add(((BulkString) array[i]).getContent());
        }
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            RedisList redisList =  RedisBaseData.getRedisDataByType(RedisList.class);
            biConsumer.accept(redisList, value);
            redisCore.put(key, redisList);
            RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
            i.getValue(redisList.size());
            ctx.writeAndFlush(i).addListener(future -> {
                key.recovery();
                value.forEach(BytesWrapper::recovery);
                i.recovery();
            });
        }
        else if (!(redisData instanceof RedisList))
        {
            Errors err = RedisBaseData.getRedisDataByType(Errors.class);
            err.setContent("wrong type");
            ctx.writeAndFlush(err).addListener(future -> err.recovery());;
        }
        else
        {
            biConsumer.accept((RedisList) redisData, value);
            redisCore.put(key, redisData);
            RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
            i.getValue(((RedisList) redisData).size());
            ctx.writeAndFlush(i).addListener(future -> {
                key.recovery();
                value.forEach(BytesWrapper::recovery);
                i.recovery();
            });
        }

    }
    @Override
    public void handle( RedisCore redisCore)
    {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            RedisList redisList =  RedisBaseData.getRedisDataByType(RedisList.class);
            biConsumer.accept(redisList, value);
            redisCore.put(key, redisList);

        }
        else if (redisData != null && !(redisData instanceof RedisList))
        {
        }
        else
        {
            biConsumer.accept((RedisList) redisData, value);
            redisCore.put(key, redisData);
        }
    }
}
