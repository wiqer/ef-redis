package com.wiqer.redis.channel;


import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;


public interface LocalChannelOption<C extends Channel> {
    /**
     * @return 返回获取tcp线程
     */
    EventLoopGroup boss();

    /**
     * @return 返回处理tcp线程
     */
    EventLoopGroup selectors();

    /**
     * @return 返回管道类型
     */
    Class<? extends C> getChannelClass();
}
