package com.wiqer.redis.command.impl.string;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.*;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mset implements WriteCommand
{
    private List<BytesWrapper> kvList;

    @Override
    public CommandType type()
    {
        return CommandType.mset;
    }

    @Override
    public void setContent(Resp[] array)
    {
        kvList = Stream.of(array).skip(1).map(resp -> ((BulkString) resp).getContent()).collect(Collectors.toList());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        for (int i = 0; i<kvList.size();i+=2) {
            RedisString redisString = RedisBaseData.getRedisDataByType(RedisString.class);
            redisString.setValue(kvList.get(i+1));
            redisCore.put(kvList.get(i),redisString);
        }
        ctx.writeAndFlush(SimpleString.OK);
    }

    @Override
    public void handle(RedisCore redisCore) {
        for (int i = 0; i<kvList.size();i+=2) {
            RedisString redisString = RedisBaseData.getRedisDataByType(RedisString.class);
            redisString.setValue(kvList.get(i+1));
            redisCore.put(kvList.get(i), redisString);
        }
    }
}
