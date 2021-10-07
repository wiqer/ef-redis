package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisList;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Errors;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class Push implements Command
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
            RedisList redisList = new RedisList();
            biConsumer.accept(redisList, value);
            redisCore.put(key, redisList);
            ctx.writeAndFlush(new RespInt(redisList.size()));
        }
        else if (redisData != null && !(redisData instanceof RedisList))
        {
            ctx.writeAndFlush(new Errors("wrong type"));
        }
        else
        {
            biConsumer.accept((RedisList) redisData, value);
            redisCore.put(key, redisData);
            ctx.writeAndFlush(new RespInt(((RedisList) redisData).size()));
        }
    }
}
