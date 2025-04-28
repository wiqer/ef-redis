package com.wiqer.redis.command.impl.list;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisList;
import com.wiqer.redis.resp.*;
import io.netty.channel.ChannelHandlerContext;

public class Lrem implements WriteCommand {
    private BytesWrapper key;
    private BytesWrapper value;

    @Override
    public CommandType type() {
        return CommandType.lrem;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
        value = ((BulkString) array[3]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisList redisList = (RedisList) redisCore.get(key);
        if (redisList == null) {
            ctx.writeAndFlush(new Errors("1"));
        } else {
            int remove = redisList.remove(value);
            ctx.writeAndFlush(new RespInt(remove));
        }

    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisList redisList = (RedisList) redisCore.get(key);
        redisList.remove(value);
    }
}
