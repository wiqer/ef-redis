package com.wiqer.redis.command.impl;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.SimpleString;
import com.wiqer.redis.util.Format;
import io.netty.channel.ChannelHandlerContext;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Incr implements WriteCommand
{
    private BytesWrapper key;

    @Override
    public CommandType type()
    {
        return CommandType.incr;
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
            RedisString stringData = new RedisString();
            BytesWrapper bytesWrapper=BytesWrapper.ZERO;
            stringData.setValue(bytesWrapper);
            redisCore.put(key, stringData);
            BulkString bulkString =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkString.setContent(bytesWrapper);
            ctx.writeAndFlush(bulkString);
        }
        else if (redisData instanceof RedisString)
        {
            try {
                BytesWrapper value = ((RedisString) redisData).getValue();
                long v= Format.parseLong(value.getByteArray(),10);
                ++v;
                BytesWrapper bytesWrapper =  RedisBaseData.getRedisDataByType(BytesWrapper.class);
                bytesWrapper.setByteArray(Format.toByteArray(v));
                ((RedisString) redisData).setValue(bytesWrapper);
                BulkString bulkString =  RedisBaseData.getRedisDataByType(BulkString.class);
                bulkString.setContent(bytesWrapper);
                ctx.writeAndFlush(bulkString);
            }catch (NumberFormatException exception){
                SimpleString vr =  RedisBaseData.getRedisDataByType(SimpleString.class);
                vr.setContent("value is not an integer or out of range");
                ctx.writeAndFlush(vr);
            }

        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null)
        {
            RedisString stringData = new RedisString(BytesWrapper.ZERO);
            redisCore.put(key, stringData);
        }
        else if (redisData instanceof RedisString)
        {
            try {
                BytesWrapper value = ((RedisString) redisData).getValue();
                long v= Format.parseLong(value.getByteArray(),10);
                ++v;
                BytesWrapper bytesWrapper =  RedisBaseData.getRedisDataByType(BytesWrapper.class);
                bytesWrapper.setByteArray(Format.toByteArray(v));
                ((RedisString) redisData).setValue(bytesWrapper);
            }catch (NumberFormatException exception){
            }
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
