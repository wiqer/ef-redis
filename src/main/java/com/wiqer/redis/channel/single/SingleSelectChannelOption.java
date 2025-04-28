package com.wiqer.redis.channel.single;

import com.wiqer.redis.aof.RingBlockingQueue;
import com.wiqer.redis.channel.LocalChannelOption;
import com.wiqer.redis.netty.channel.nio.NioSingleEventLoopGroup;
import com.wiqer.redis.netty.channel.socket.NioSingleServerSocketChannel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleSelectChannelOption implements LocalChannelOption {
    private final NioSingleEventLoopGroup single;

    public SingleSelectChannelOption(NioSingleEventLoopGroup single) {
        this.single = single;
    }

    public SingleSelectChannelOption() {
        this.single = new NioSingleEventLoopGroup(new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new RingBlockingQueue<>(8888, 888888), new DefaultThreadFactory("NioSingleEventLoopGroup_boss")));

    }

    @Override
    public EventLoopGroup boss() {
        return this.single;
    }

    @Override
    public EventLoopGroup selectors() {
        return this.single;
    }

    @Override
    public Class getChannelClass() {
        return NioSingleServerSocketChannel.class;
    }
}
