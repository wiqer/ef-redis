package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.SimpleString;
import io.netty.channel.ChannelHandlerContext;

public class Auth implements Command
{
    private String password;

    @Override
    public CommandType type()
    {
        return CommandType.auth;
    }

    @Override
    public void setContent(Resp[] array)
    {
        BulkString blukStrings = (BulkString) array[1];
        byte[]     content     = blukStrings.getContent().getByteArray();
        if (content.length == 0)
        {
            password = "";
        }
        else
        {
            password = new String(content);
        }
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {

        ctx.writeAndFlush(SimpleString.OK);
    }
}
