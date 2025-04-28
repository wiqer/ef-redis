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

public class Ttl implements WriteCommand {
    private BytesWrapper key;

    @Override
    public CommandType type() {
        return CommandType.ttl;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            ctx.writeAndFlush(new RespInt(-2));
        } else if (redisData.timeout() == -1) {
            ctx.writeAndFlush(new RespInt(-1));
        } else {
            long second = (redisData.timeout() - System.currentTimeMillis()) / 1000;
            ctx.writeAndFlush(new RespInt((int) second));
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
        } else if (redisData.timeout() == -1) {
        } else {
            long second = (redisData.timeout() - System.currentTimeMillis()) / 1000;
        }
    }
}
