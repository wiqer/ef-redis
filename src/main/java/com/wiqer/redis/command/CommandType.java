package com.wiqer.redis.command;

import com.wiqer.redis.command.impl.*;
import com.wiqer.redis.command.impl.hash.*;
import com.wiqer.redis.command.impl.list.*;
import com.wiqer.redis.command.impl.set.*;
import com.wiqer.redis.command.impl.string.*;
import com.wiqer.redis.command.impl.zset.*;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
public enum CommandType {
    auth(Auth::new), config(Config::new), scan(Scan::new),//
    info(Info::new), client(Client::new), set(Set::new), type(Type::new),//
    ttl(Ttl::new), get(Get::new), quit(Quit::new),//
    setnx(SetNx::new), lpush(Lpush::new), lrange(Lrange::new), lrem(Lrem::new), rpush(Rpush::new), del(Del::new), sadd(Sadd::new),//
    sscan(Sscan::new), srem(Srem::new), hset(Hset::new), hscan(Hscan::new), hdel(Hdel::new),hget(Hget::new),hgetall(HgetAll::new),hexists(Hexists::new),//
    zadd(Zadd::new), zrevrange(Zrevrange::new), zrem(Zrem::new), setex(SetEx::new), exists(Exists::new), expire(Expire::new),
    ping(Ping::new), select(Select::new), keys(Keys::new), incr(Incr::new), decr(Decr::new), mset(Mset::new), mget(Mget::new),
    spop(Spop::new),lpop(Lpop::new),rpop(Rpop::new),getrange(GetRange::new),append(Append::new),llen(Llen::new),scard(Scard::new),zcard(Zcard::new),flushdb(Flushdb::new),strlen(Strlen::new),
    //
    ;

    private final Supplier<Command> supplier;

    CommandType(Supplier<Command> supplier) {
        this.supplier = supplier;
    }

}
