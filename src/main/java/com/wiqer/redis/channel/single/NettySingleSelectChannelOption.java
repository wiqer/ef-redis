package com.wiqer.redis.channel.single;

import com.wiqer.redis.channel.LocalChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NettySingleSelectChannelOption implements LocalChannelOption {
    private final NioEventLoopGroup single;

    public NettySingleSelectChannelOption(NioEventLoopGroup single) {
        this.single = single;
    }
    public NettySingleSelectChannelOption()
    {
        this.single = new NioEventLoopGroup( new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_boss_" + index.getAndIncrement());
            }
        });

    }
    @Override
    public EventLoopGroup boss() {
        return  this.single;
    }

    @Override
    public EventLoopGroup selectors() {
        return  this.single;
    }

    @Override
    public Class getChannelClass() {
        return NioServerSocketChannel.class;
    }
}
