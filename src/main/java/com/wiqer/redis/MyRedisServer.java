package com.wiqer.redis;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRedisServer implements RedisServer
{
    private final ServerBootstrap serverBootstrap=new ServerBootstrap();;
    private final RedisCore redisCore =  new RedisCoreImpl();
    private       DefaultEventExecutorGroup workerGroup;
    private final NioEventLoopGroup boss;
    private final NioEventLoopGroup selectors;
    private final  EventExecutorGroup redisSingleEventExecutor;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    public MyRedisServer()
    {
        this.boss = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_boss_" + index.getAndIncrement());
            }
        });

        this.selectors = new NioEventLoopGroup(16, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_selector_" + index.getAndIncrement());
            }
        });
        this.redisSingleEventExecutor=new NioEventLoopGroup(1);
//        JnetWorkerImpl jnetWorker = new JnetWorkerImpl("redis-singleThread");
//        jnetWorker.start();
//        workerGroup = () -> jnetWorker;
    }

    public static void main(String[] args)
    {
        new MyRedisServer().start(6378);
    }

    @Override
    public void start(int port)
    {
        start0();
    }

    @Override
    public void close()
    {
        try {
            boss.shutdownGracefully();
            selectors.shutdownGracefully();


        }catch (Exception e) {

        }
    }
    public void start0() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(16, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_worker_" + index.getAndIncrement());
            }
        });

        serverBootstrap.group(boss, selectors)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, true)//false
                .childOption(ChannelOption.TCP_NODELAY, true)
//                .childOption(ChannelOption.SO_SNDBUF, 65535)
//                .childOption(ChannelOption.SO_RCVBUF, 65535)
//                .localAddress(new InetSocketAddress(serverConfig.getListenAddr(), serverConfig.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        channelPipeline.addLast(defaultEventExecutorGroup,
                                new ResponseEncoder(),
                                new CommandDecoder(),
                                /*心跳*/
                                new IdleStateHandler(0, 0, 20)
                        );
                        channelPipeline.addLast(redisSingleEventExecutor,new CommandHandler(redisCore)) ;
                    }
                });

        try {
            ChannelFuture sync = serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();

        } catch (InterruptedException e) {
//
            throw new RuntimeException(e);
        }

    }

    @Override
    public RedisCore getRedisCore()
    {
        return redisCore;
    }
}
