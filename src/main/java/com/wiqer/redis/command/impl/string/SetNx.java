package com.wiqer.redis.command.impl.string;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class SetNx implements WriteCommand
{
    private BytesWrapper key;
    private BytesWrapper value;

    @Override
    public CommandType type()
    {
        return CommandType.setnx;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        value = ((BulkString) array[2]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        boolean exist = redisCore.exist(key);
        if (exist)
        {
            RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
            i.getValue(0);
            ctx.writeAndFlush(i).addListener(future -> i.recovery());
        }
        else
        {
            RedisString stringData = RedisBaseData.getRedisDataByType(RedisString.class);
            stringData.setValue(value);
            redisCore.put(key, stringData);
            RespInt i = RedisBaseData.getRedisDataByType(RespInt.class);
            i.getValue(1);
            ctx.writeAndFlush(i).addListener(future -> i.recovery());
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        boolean exist = redisCore.exist(key);
        if (exist)
        {
        }
        else
        {
            RedisString redisString = RedisBaseData.getRedisDataByType(RedisString.class);
            redisString.setValue(value);
            redisCore.put(key, redisString);

        }
    }
}
