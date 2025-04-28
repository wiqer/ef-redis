package com.wiqer.redis;

import com.wiqer.redis.aof.Aof;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandFactory;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.resp.*;
import com.wiqer.redis.util.PropertiesUtil;
import com.wiqer.redis.util.TRACEID;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;



/**
 * @author lilan
 */
@Slf4j
public class CommandDecoder extends LengthFieldBasedFrameDecoder {
    //最大 max 50mb
    private static final int MAX_FRAME_LENGTH = Integer.MAX_VALUE>>>2;
    private Aof aof = null;

    //    static {
//        if(PropertiesUtil.getAppendOnly()) {
//            aof=new Aof();
//        }
//    }
    public CommandDecoder(Aof aof) {
        this();
        this.aof = aof;
    }

    public CommandDecoder() {
        super(MAX_FRAME_LENGTH, 0, 4);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

        TRACEID.newTraceId();
        while (in.readableBytes() != 0) {
            int mark = in.readerIndex();
            try {
                Resp resp = Resp.decode(in);
                if (!(resp instanceof RespArray || resp instanceof SimpleString)) {
                    throw new IllegalStateException("客户端发送的命令应该只能是Resp Array 和 单行命令 类型");
                }
                Command command = null;
                if (resp instanceof RespArray) {
                    command = CommandFactory.from((RespArray) resp);
                } else if (resp instanceof SimpleString) {
                    command = CommandFactory.from((SimpleString) resp);

                }
                if (command == null) {
                    //取出命令
                    ctx.writeAndFlush(new Errors("unsupport command:" + ((BulkString) ((RespArray) resp).getArray()[0]).getContent().toUtf8String()));
                } else {
                    if (aof != null && command instanceof WriteCommand) {
                        aof.put(resp);
                    }
                    return command;
                }
            } catch (Exception e) {
                in.readerIndex(mark);
                log.error("解码命令", e);
                break;
            }
        }
        return null;
    }


}
