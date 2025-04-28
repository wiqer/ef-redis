package com.wiqer.redis.command;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author lilan
 */
public interface WriteCommand extends Command {
    /**
     * for aof
     *
     * @param redisCore
     */
    void handle(RedisCore redisCore);

}
