package com.wiqer.redis.command.impl.set;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisSet;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class Scard implements Command {
    private BytesWrapper key;

    @Override
    public CommandType type() {
        return CommandType.scard;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            ctx.writeAndFlush(new RespInt(0));
        } else if (redisData instanceof RedisSet) {
            int size = ((RedisSet) redisData).size();
            ctx.writeAndFlush(new RespInt(size));
        } else {
            throw new UnsupportedOperationException();
        }
    }
} 