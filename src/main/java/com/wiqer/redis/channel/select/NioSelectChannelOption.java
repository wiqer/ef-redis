package com.wiqer.redis.channel.select;

import com.wiqer.redis.channel.LocalChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NioSelectChannelOption implements LocalChannelOption {
    private final NioEventLoopGroup boss;
    private final NioEventLoopGroup selectors;

    public NioSelectChannelOption(NioEventLoopGroup boss, NioEventLoopGroup selectors) {
        this.boss = boss;
        this.selectors = selectors;
    }

    public NioSelectChannelOption() {
        this.boss = new NioEventLoopGroup(4, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_boss_" + index.getAndIncrement());
            }
        });

        this.selectors = new NioEventLoopGroup(Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors())), new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_selector_" + index.getAndIncrement());
            }
        });
    }

    @Override
    public EventLoopGroup boss() {
        return this.boss;
    }

    @Override
    public EventLoopGroup selectors() {
        return this.selectors;
    }

    @Override
    public Class getChannelClass() {
        return NioServerSocketChannel.class;
    }
}
