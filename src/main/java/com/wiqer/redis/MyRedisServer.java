package com.wiqer.redis;


import com.wiqer.redis.aof.Aof;
import com.wiqer.redis.aof.RingBlockingQueue;
import com.wiqer.redis.channel.DefaultChannelSelectStrategy;
import com.wiqer.redis.channel.LocalChannelOption;
import com.wiqer.redis.channel.single.SingleSelectChannelOption;
import com.wiqer.redis.netty.channel.nio.NioSingleEventLoopGroup;
import com.wiqer.redis.util.PropertiesUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;



import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 */
@Slf4j
public class MyRedisServer implements RedisServer {
    private final RedisCore redisCore = new RedisCoreImpl();
    private final ServerBootstrap serverBootstrap = new ServerBootstrap();
    private final EventExecutorGroup redisSingleEventExecutor;
    private final LocalChannelOption<ServerSocketChannel> channelOption;
    private Aof aof;

    public MyRedisServer() {
        channelOption = new DefaultChannelSelectStrategy().select();
        this.redisSingleEventExecutor = new NioEventLoopGroup(1);
    }

    public MyRedisServer(LocalChannelOption<ServerSocketChannel> channelOption) {
        this.channelOption = channelOption;
        this.redisSingleEventExecutor = new NioSingleEventLoopGroup(new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new RingBlockingQueue<>(8888, 888888), new DefaultThreadFactory("NioSingleEventLoopGroup_biz")));
    }

    public static void main(String[] args) {
        new MyRedisServer(new SingleSelectChannelOption()).start();
    }

    @Override
    public void start() {
        if (PropertiesUtil.getAppendOnly()) {
            aof = new Aof(this.redisCore);
        }
        start0();
    }

    @Override
    public void close() {
        try {
            channelOption.boss().shutdownGracefully();
            channelOption.selectors().shutdownGracefully();
            redisSingleEventExecutor.shutdownGracefully();
        } catch (Exception ignored) {
            log.warn("Exception!", ignored);
        }
    }

    public void start0() {


        serverBootstrap.group(channelOption.boss(), channelOption.selectors())
                .channel(channelOption.getChannelClass())
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //false
                .option(ChannelOption.SO_KEEPALIVE, PropertiesUtil.getTcpKeepAlive())
//                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, 65535)
                .childOption(ChannelOption.SO_RCVBUF, 65535)
                .localAddress(new InetSocketAddress(PropertiesUtil.getNodeAddress(), PropertiesUtil.getNodePort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        channelPipeline.addLast(
                                new ResponseEncoder(),
                                new CommandDecoder(aof)//,
//                                /*心跳,管理长连接*/
//                                new IdleStateHandler(0, 0, 20)
                        );
                        channelPipeline.addLast(//redisSingleEventExecutor,
                                new CommandHandler(redisCore));
                    }
                });
        redisCore.startTllTask(PropertiesUtil.getHz(),PropertiesUtil.getMaxMemory());

        try {
            ChannelFuture sync = serverBootstrap.bind().sync();
            log.info(sync.channel().localAddress().toString());
        } catch (InterruptedException e) {
//
            log.warn("Interrupted!", e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public RedisCore getRedisCore() {
        return redisCore;
    }
}
