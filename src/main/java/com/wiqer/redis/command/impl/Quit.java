package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.SimpleString;
import io.netty.channel.ChannelHandlerContext;

public class Quit implements Command {
    @Override
    public CommandType type() {
        return CommandType.quit;
    }

    @Override
    public void setContent(Resp[] array) {
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        ctx.writeAndFlush(SimpleString.OK);
        ctx.close();
    }
}
