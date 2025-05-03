package com.wiqer.redis.channel.local;

import com.wiqer.redis.channel.LocalChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalServerChannel;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultChannelOption implements LocalChannelOption<LocalServerChannel> {

    private final DefaultEventLoopGroup boss;
    private final DefaultEventLoopGroup selectors;

    public DefaultChannelOption() {
        this.boss = new DefaultEventLoopGroup(4, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_boss_" + index.getAndIncrement());
            }
        });
        this.selectors = new DefaultEventLoopGroup(8, new ThreadFactory() {
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
    public Class<LocalServerChannel> getChannelClass() {
        return LocalServerChannel.class;
    }
}
