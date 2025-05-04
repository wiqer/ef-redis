package com.wiqer.redis.command.impl.list;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.command.impl.Push;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisList;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Errors;
import com.wiqer.redis.resp.Resp;
import io.netty.channel.ChannelHandlerContext;

public class Lpop implements WriteCommand {
    private BytesWrapper key;

    @Override
    public CommandType type() {
        return CommandType.lpop;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            ctx.writeAndFlush(new Errors("1"));
        }else if (redisData instanceof RedisList) {
            RedisList list = (RedisList) redisData;
            BytesWrapper val = list.lpop();
            ctx.writeAndFlush(new BulkString(val));
        }else {
            throw new IllegalArgumentException("类型不匹配");
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
        }else if (redisData instanceof RedisList) {
            RedisList list = (RedisList) redisData;
            list.lpop();
        }else {
            throw new IllegalArgumentException("类型不匹配");
        }
    }
}
