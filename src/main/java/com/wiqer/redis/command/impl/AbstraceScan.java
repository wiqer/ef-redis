package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import io.netty.channel.ChannelHandlerContext;

public abstract class AbstraceScan implements Command {

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        Resp[] array = new Resp[2];
        BulkString blukStrings = new BulkString(new BytesWrapper("0".getBytes(CHARSET)));
        array[0] = blukStrings;
        array[1] = get(redisCore);
        ctx.writeAndFlush(new RespArray(array));
    }

    protected abstract RespArray get(RedisCore redisCore);
}
