package com.wiqer.redis.command.impl;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.SimpleString;
import io.netty.channel.ChannelHandlerContext;

public class Flushdb implements WriteCommand {

    @Override
    public CommandType type() {
        return CommandType.flushdb;
    }

    @Override
    public void setContent(Resp[] array) {
        // FLUSHDB命令不需要参数
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        redisCore.cleanAll();
        ctx.writeAndFlush(SimpleString.OK);
    }

    @Override
    public void handle(RedisCore redisCore) {
        redisCore.cleanAll();
    }
} 