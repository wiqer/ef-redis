package com.wiqer.redis;



import com.wiqer.redis.aof.Aof;
import com.wiqer.redis.util.PropertiesUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.log4j.Logger;


import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRedisServer implements RedisServer
{
    private static final Logger LOGGER = Logger.getLogger(MyRedisServer.class);
    private final ServerBootstrap serverBootstrap=new ServerBootstrap();
    private final RedisCore redisCore =  new RedisCoreImpl();
    private final NioEventLoopGroup boss;
    private final NioEventLoopGroup selectors;
    private final  EventExecutorGroup redisSingleEventExecutor;
    private Aof aof;
    public MyRedisServer()
    {
        this.boss = new NioEventLoopGroup(4, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_boss_" + index.getAndIncrement());
            }
        });

        this.selectors = new NioEventLoopGroup(8, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_selector_" + index.getAndIncrement());
            }
        });
        this.redisSingleEventExecutor=new NioEventLoopGroup(1);
    }

    public static void main(String[] args)
    {
        new MyRedisServer().start();
    }

    @Override
    public void start()
    {
        if(PropertiesUtil.getAppendOnly()) {
            aof=new Aof(this.redisCore);
        }
        start0();
    }

    @Override
    public void close()
    {
        try {
            boss.shutdownGracefully();
            selectors.shutdownGracefully();
            redisSingleEventExecutor.shutdownGracefully();
        }catch (Exception ignored) {
            LOGGER.warn( "Exception!", ignored);
        }
    }
    public void start0() {


        serverBootstrap.group(boss, selectors)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                //false
                .option(ChannelOption.SO_KEEPALIVE, PropertiesUtil.getTcpKeepAlive())
//                .childOption(ChannelOption.TCP_NODELAY, true)
//                .childOption(ChannelOption.SO_SNDBUF, 65535)
//                .childOption(ChannelOption.SO_RCVBUF, 65535)
                .localAddress(new InetSocketAddress(PropertiesUtil.getNodeAddress(), PropertiesUtil.getNodePort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        channelPipeline.addLast(
                                new ResponseEncoder(),
                                new CommandDecoder(aof)//,
//                                /*心跳,管理长连接*/
//                                new IdleStateHandler(0, 0, 20)
                        );
                        channelPipeline.addLast(redisSingleEventExecutor,new CommandHandler(redisCore)) ;
                    }
                });

        try {
            ChannelFuture sync = serverBootstrap.bind().sync();
            LOGGER.info(sync.channel().localAddress().toString());
        } catch (InterruptedException e) {
//
            LOGGER.warn( "Interrupted!", e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public RedisCore getRedisCore()
    {
        return redisCore;
    }
}
