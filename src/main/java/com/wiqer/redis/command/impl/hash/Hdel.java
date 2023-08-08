package com.wiqer.redis.command.impl.hash;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisHash;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Hdel implements WriteCommand
{
    private BytesWrapper       key;
    private List<BytesWrapper> fields;

    @Override
    public CommandType type()
    {
        return CommandType.hdel;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        fields = Stream.of(array).skip(2).map(resp -> ((BulkString) resp).getContent()).collect(Collectors.toList());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisHash redisHash = (RedisHash) redisCore.get(key);
        int       del       = redisHash.del(fields);
        RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
        i.setValue(del);
        ctx.writeAndFlush(i);
        key.recovery();
        fields.forEach(BytesWrapper::recovery);
        i.recovery();
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisHash redisHash = (RedisHash) redisCore.get(key);
        redisHash.del(fields);
    }
}
