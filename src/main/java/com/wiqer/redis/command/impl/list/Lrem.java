package com.wiqer.redis.command.impl.list;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisList;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class Lrem implements WriteCommand
{
    private BytesWrapper key;
    private BytesWrapper value;

    @Override
    public CommandType type()
    {
        return CommandType.lrem;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        value = ((BulkString) array[3]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisList redisList = (RedisList) redisCore.get(key);
        int       remove    = redisList.remove(value);
        RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
        i.setValue(remove);
        ctx.writeAndFlush(i);
        key.recovery();
        value.recovery();
        i.recovery();
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisList redisList = (RedisList) redisCore.get(key);
        redisList.remove(value);
    }
}
