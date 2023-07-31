package com.wiqer.redis.command.impl;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
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
            BytesWrapper bytesWrapper=new BytesWrapper("0".getBytes(UTF_8));
            stringData.setValue(bytesWrapper);
            redisCore.put(key, stringData);
            ctx.writeAndFlush(new BulkString(bytesWrapper));
        }
        else if (redisData instanceof RedisString)
        {
            try {
                BytesWrapper value = ((RedisString) redisData).getValue();
                long v= Format.parseLong(value.getByteArray(),10);
                ++v;
                BytesWrapper bytesWrapper= new BytesWrapper(Format.toByteArray(v));
                ((RedisString) redisData).setValue(bytesWrapper);
                ctx.writeAndFlush(new BulkString(bytesWrapper));
            }catch (NumberFormatException exception){
                ctx.writeAndFlush(new SimpleString("value is not an integer or out of range"));
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
            RedisString stringData = new RedisString(new BytesWrapper("0".getBytes(UTF_8)));
            redisCore.put(key, stringData);
        }
        else if (redisData instanceof RedisString)
        {
            try {
                BytesWrapper value = ((RedisString) redisData).getValue();
                long v= Format.parseLong(value.getByteArray(),10);
                ++v;
                ((RedisString) redisData).setValue(new BytesWrapper(Format.toByteArray(v)));
            }catch (NumberFormatException exception){
            }
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
