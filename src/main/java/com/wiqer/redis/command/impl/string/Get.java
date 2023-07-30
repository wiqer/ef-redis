package com.wiqer.redis.command.impl.string;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import io.netty.channel.ChannelHandlerContext;

public class Get implements Command
{
    private BytesWrapper key;

    @Override
    public CommandType type()
    {
        return CommandType.get;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            ctx.writeAndFlush(BulkString.NullBulkString);
        }
        else if (redisData instanceof RedisString)
        {
            BytesWrapper value = ((RedisString) redisData).getValue();
            BulkString bulkString =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkString.setContent(value);
            ctx.writeAndFlush(bulkString);
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
