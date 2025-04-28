package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class Expire implements WriteCommand {
    private BytesWrapper key;
    private int second;

    @Override
    public CommandType type() {
        return CommandType.expire;
    }


    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
        second = Integer.parseInt(((BulkString) array[2]).getContent().toUtf8String());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            ctx.writeAndFlush(new RespInt(0));
        } else {
            redisData.setTimeout(System.currentTimeMillis() + (second * 1000));
            ctx.writeAndFlush(new RespInt(1));
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
        } else {
            redisData.setTimeout(System.currentTimeMillis() + (second * 1000));
        }
    }
}
