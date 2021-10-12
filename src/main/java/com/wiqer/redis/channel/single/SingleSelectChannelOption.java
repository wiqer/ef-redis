package com.wiqer.redis.channel.single;

import com.wiqer.redis.channel.LocalChannelOption;
import com.wiqer.redis.netty.channel.nio.NioSingleEventLoopGroup;
import com.wiqer.redis.netty.channel.socket.NioSingleServerSocketChannel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleSelectChannelOption implements LocalChannelOption {
    private final NioSingleEventLoopGroup single;

    public SingleSelectChannelOption(NioSingleEventLoopGroup single) {
        this.single = single;
    }
    public SingleSelectChannelOption()
    {
        this.single = new NioSingleEventLoopGroup( new ThreadFactory() {
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
        return NioSingleServerSocketChannel.class;
    }
}
