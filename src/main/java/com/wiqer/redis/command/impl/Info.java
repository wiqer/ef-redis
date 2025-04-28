package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import io.netty.channel.ChannelHandlerContext;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Info implements Command {
    @Override
    public CommandType type() {
        return CommandType.info;
    }

    @Override
    public void setContent(Resp[] array) {
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        List<String> list = new ArrayList<>();
        list.add("redis_version:jfire_redis_mock");
        list.add("os:" + System.getProperty("os.name"));
        list.add("process_id:" + getPid());
        Optional<String> reduce = list.stream().map(name -> name + "\r\n").reduce((first, second) -> first + second);
        String s = reduce.get();
        ctx.writeAndFlush(new BulkString(new BytesWrapper(s.getBytes(CHARSET))));
    }

    private String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        return pid;
    }
}
