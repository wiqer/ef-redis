package com.wiqer.redis.command.impl.hash;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisHash;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class Hexists implements Command {
    private BytesWrapper key;
    private BytesWrapper field;

    @Override
    public CommandType type() {
        return CommandType.hexists;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
        field = ((BulkString) array[2]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            ctx.writeAndFlush(new RespInt(0));
        } else if (redisData instanceof RedisHash) {
            boolean exists = ((RedisHash) redisData).getMap().containsKey(field);
            ctx.writeAndFlush(new RespInt(exists ? 1 : 0));
        } else {
            throw new UnsupportedOperationException();
        }
    }
} 