package com.wiqer.redis.command;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.resp.Resp;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Command
{
    Charset CHARSET = StandardCharsets.UTF_8;
    Logger  LOGGER  = LoggerFactory.getLogger(Command.class);

    CommandType type();

    void setContent(Resp[] array);

    void handle(ChannelHandlerContext ctx, RedisCore redisCore);
}
