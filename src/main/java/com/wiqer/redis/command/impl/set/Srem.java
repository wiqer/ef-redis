package com.wiqer.redis.command.impl.set;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisSet;
import com.wiqer.redis.resp.*;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Srem implements WriteCommand
{
    private BytesWrapper       key;
    private List<BytesWrapper> members;

    @Override
    public CommandType type()
    {
        return CommandType.srem;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        members = Stream.of(array).skip(2).map(resp -> ((BulkString) resp).getContent()).collect(Collectors.toList());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisSet redisSet = (RedisSet) redisCore.get(key);
        if(redisSet == null){
            ctx.writeAndFlush(new Errors("1"));
        }else {
            int      srem     = redisSet.srem(members);
            ctx.writeAndFlush(new RespInt(srem));
        }

    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisSet redisSet = (RedisSet) redisCore.get(key);
        redisSet.srem(members);
    }
}
