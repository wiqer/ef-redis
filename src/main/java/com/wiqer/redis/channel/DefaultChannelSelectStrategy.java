package com.wiqer.redis.channel;

import com.wiqer.redis.channel.epoll.EpollChannelOption;
import com.wiqer.redis.channel.kqueue.KqueueChannelOption;
import com.wiqer.redis.channel.select.NioSelectChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.socket.ServerSocketChannel;

public class DefaultChannelSelectStrategy implements ChannelSelectStrategy {
    @Override
    public LocalChannelOption<ServerSocketChannel> select() {

        if (KQueue.isAvailable()) {
            return new KqueueChannelOption();
        }
        if (Epoll.isAvailable()) {
            return new EpollChannelOption();
        }
        return new NioSelectChannelOption();
    }
}
