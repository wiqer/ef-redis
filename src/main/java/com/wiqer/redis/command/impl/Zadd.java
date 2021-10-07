package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisZset;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

public class Zadd implements Command
{
    private BytesWrapper            key;
    private List<RedisZset.ZsetKey> keys;

    @Override
    public CommandType type()
    {
        return CommandType.zadd;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        keys = new ArrayList<>();
        for (int i = 2; i + 1 < array.length; i += 2)
        {
            long         score  = Long.parseLong(((BulkString) array[i]).getContent().toUtf8String());
            BytesWrapper member = ((BulkString) array[i + 1]).getContent();
            keys.add(new RedisZset.ZsetKey(member, score));
        }
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            RedisZset redisZset = new RedisZset();
            int       add       = redisZset.add(keys);
            redisCore.put(key, redisZset);
            ctx.writeAndFlush(new RespInt(add));
        }
        else if (redisData instanceof RedisZset)
        {
            RedisZset redisZset = (RedisZset) redisData;
            int       add       = redisZset.add(keys);
            ctx.writeAndFlush(new RespInt(add));
        }
        else
        {
            throw new UnsupportedOperationException("类型不匹配");
        }
    }
}
