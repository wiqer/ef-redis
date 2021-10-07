package com.wiqer.redis.command.impl;

import com.wiqer.redis.command.CommandType;

public class Rpush extends Push
{

    public Rpush()
    {
        super((redisList, values) -> redisList.rpush(values));
    }

    @Override
    public CommandType type()
    {
        return CommandType.rpush;
    }
}
