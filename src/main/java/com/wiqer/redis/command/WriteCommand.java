package com.wiqer.redis.command;

import com.wiqer.redis.RedisCore;

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
