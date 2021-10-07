package com.wiqer.redis.command;

import com.wiqer.redis.command.impl.*;

import java.util.function.Supplier;

public enum CommandType
{
    auth(Auth::new), config(Config::new), scan(Scan::new),//
    info(Info::new), client(Client::new), set(Set::new), type(Type::new),//
    ttl(Ttl::new), get(Get::new), quit(Quit::new),//
    setnx(SetNx::new), lpush(Lpush::new), lrange(Lrange::new), lrem(Lrem::new), rpush(Rpush::new), del(Del::new), sadd(Sadd::new),//
    sscan(Sscan::new), srem(Srem::new), hset(Hset::new), hscan(Hscan::new), hdel(Hdel::new),//
    zadd(Zadd::new), zrevrange(Zrevrange::new), zrem(Zrem::new), setex(SetEx::new), exists(Exists::new), expire(Expire::new),
    //
    ;

    private final Supplier<Command> supplier;

    CommandType(Supplier supplier)
    {
        this.supplier = supplier;
    }

    public Supplier<Command> getSupplier()
    {
        return supplier;
    }
}
