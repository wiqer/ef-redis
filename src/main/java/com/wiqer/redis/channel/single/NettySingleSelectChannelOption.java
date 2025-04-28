package com.wiqer.redis.channel.single;

import com.wiqer.redis.aof.RingBlockingQueue;
import com.wiqer.redis.channel.LocalChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NettySingleSelectChannelOption implements LocalChannelOption {
    private final NioEventLoopGroup single;

    public NettySingleSelectChannelOption(NioEventLoopGroup single) {
        this.single = single;
    }

    public NettySingleSelectChannelOption() {
        this.single = new NioEventLoopGroup(1, new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new RingBlockingQueue<>(8888, 888888), new DefaultThreadFactory("NioEventLoopGroup_boss")));

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
        return NioServerSocketChannel.class;
    }
}
