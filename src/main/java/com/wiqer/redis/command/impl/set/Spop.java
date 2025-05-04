package com.wiqer.redis.command.impl.set;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisSet;
import com.wiqer.redis.resp.*;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Spop implements WriteCommand {
    int member;
    private BytesWrapper key;

    @Override
    public CommandType type() {
        return CommandType.spop;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
        if(array.length == 3) {
            member = Integer.parseInt(((BulkString) array[2]).getContent().toUtf8String());
        }else {
            member = 1;
        }

    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        if(member < 1) {
            ctx.writeAndFlush(new Errors("1"));
        }
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            ctx.writeAndFlush(new Errors("1"));
        } else if (redisData instanceof RedisSet) {
            RedisSet redisSet = (RedisSet) redisData;
            Set<BytesWrapper> spop = redisSet.spop(member);
            ctx.writeAndFlush(new RespArray(spop.stream().map(BulkString::new).toArray(Resp[]::new)));
        } else {
            throw new IllegalArgumentException("类型不匹配");
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {

        } else if (redisData instanceof RedisSet) {
            RedisSet redisSet = (RedisSet) redisData;
            redisSet.spop(member);
        } else {
            throw new IllegalArgumentException("类型不匹配");
        }
    }
}
