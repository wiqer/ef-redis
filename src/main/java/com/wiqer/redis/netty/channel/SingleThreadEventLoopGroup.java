package com.wiqer.redis.netty.channel;

import io.netty.channel.*;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import com.wiqer.redis.netty.util.concurrent.SingleThreadEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
@Slf4j
public abstract class SingleThreadEventLoopGroup  extends SingleThreadEventExecutorGroup implements EventLoopGroup {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SingleThreadEventLoopGroup.class);

    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        DEFAULT_EVENT_LOOP_THREADS = 1;

        if (log.isDebugEnabled()) {
            log.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
    }

    /**
     * @see SingleThreadEventLoopGroup#SingleThreadEventLoopGroup(Executor, Object...)
     */
    protected SingleThreadEventLoopGroup(Executor executor, Object... args) {
        super(DEFAULT_EVENT_LOOP_THREADS, executor, args);
    }

    /**
     * @see SingleThreadEventLoopGroup#SingleThreadEventLoopGroup(ThreadFactory, Object...)
     */
    protected SingleThreadEventLoopGroup(ThreadFactory threadFactory, Object... args) {
        super(DEFAULT_EVENT_LOOP_THREADS, threadFactory, args);
    }

    /**
     * @see SingleThreadEventLoopGroup#SingleThreadEventLoopGroup( Executor,
     * EventExecutorChooserFactory, Object...)
     */
    protected SingleThreadEventLoopGroup( Executor executor, EventExecutorChooserFactory chooserFactory,
                                        Object... args) {
        super(DEFAULT_EVENT_LOOP_THREADS, executor, chooserFactory, args);
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass(), Thread.MAX_PRIORITY);
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    protected abstract EventLoop newChild(Executor executor, Object... args) throws Exception;

    @Override
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    @Override
    public ChannelFuture register(ChannelPromise promise) {
        return next().register(promise);
    }

    @Deprecated
    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return next().register(channel, promise);
    }

}
