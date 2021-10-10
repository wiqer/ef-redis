package com.wiqer.redis.channel.kqueue;

import com.wiqer.redis.channel.LocalChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class KqueueChannelOption implements LocalChannelOption {

    private final KQueueEventLoopGroup boss;
    private final KQueueEventLoopGroup selectors;

    public KqueueChannelOption(KQueueEventLoopGroup boss, KQueueEventLoopGroup selectors) {
        this.boss = boss;
        this.selectors = selectors;
    }
    public KqueueChannelOption()
    {
        this.boss = new KQueueEventLoopGroup(4, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_boss_" + index.getAndIncrement());
            }
        });

        this.selectors = new KQueueEventLoopGroup(8, new ThreadFactory() {
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
        return  this.selectors;
    }

    @Override
    public Class getChannelClass() {
        return KQueueServerSocketChannel.class;
    }
}
