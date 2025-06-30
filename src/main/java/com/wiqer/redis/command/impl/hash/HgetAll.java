package com.wiqer.redis.command.impl.hash;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisHash;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

public class HgetAll implements Command {
    private BytesWrapper key;

    @Override
    public CommandType type() {
        return CommandType.hgetall;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            ctx.writeAndFlush(new RespArray(new Resp[0]));
        } else if (redisData instanceof RedisHash) {
            RedisHash redisHash = (RedisHash) redisData;
            List<Resp> result = new ArrayList<>();
            
            // 将Map中的所有字段和值转换为Resp数组
            redisHash.getMap().forEach((field, value) -> {
                result.add(new BulkString(field));
                result.add(new BulkString(value));
            });
            
            ctx.writeAndFlush(new RespArray(result.toArray(new Resp[0])));
        } else {
            throw new UnsupportedOperationException();
        }
    }
} 