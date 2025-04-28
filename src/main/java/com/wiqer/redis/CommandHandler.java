package com.wiqer.redis;


import com.wiqer.redis.command.Command;
import com.wiqer.redis.util.TRACEID;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;


// ReadProcessor<Command>
@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<Command> {


    private final RedisCore redisCore;

    public CommandHandler(RedisCore redisCore) {
        this.redisCore = redisCore;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command command) throws Exception {
        String traceId = TRACEID.currentTraceId();
        log.debug("traceId:" + traceId + " 本次处理的命令：" + command.type().name());
        try {
            command.handle(ctx, redisCore);

        } catch (Exception e) {
            log.error("处理数据时", e);
        }

        log.debug("traceId:" + traceId + " 命令处理完毕");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(" ExceptionCaught：", cause);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        ctx.flush();
    }
}
