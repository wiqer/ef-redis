package com.wiqer.redis.command.impl.string;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.SimpleString;
import io.netty.channel.ChannelHandlerContext;

public class SetEx implements WriteCommand
{
    private BytesWrapper key;
    private int          seconds;
    private BytesWrapper value;

    @Override
    public CommandType type()
    {
        return CommandType.setex;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        seconds = Integer.parseInt(((BulkString) array[2]).getContent().toUtf8String());
        value = ((BulkString) array[3]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisString redisString = new RedisString();
        redisString.setValue(value);
        redisString.setTimeout(System.currentTimeMillis() + (seconds * 1000L));
        redisCore.put(key, redisString);
        ctx.writeAndFlush(SimpleString.OK);
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisString redisString = new RedisString();
        redisString.setValue(value);
        redisString.setTimeout(System.currentTimeMillis() + (seconds * 1000L));
        redisCore.put(key, redisString);
    }
}
