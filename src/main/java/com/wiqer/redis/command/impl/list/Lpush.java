package com.wiqer.redis.command.impl.list;

import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.impl.Push;
import com.wiqer.redis.datatype.RedisList;

public class Lpush extends Push {

    public Lpush() {
        super(RedisList::lpush);
    }

    @Override
    public CommandType type() {
        return CommandType.lpush;
    }
}
