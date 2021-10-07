package com.wiqer.redis;

import com.wiqer.redis.resp.Resp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommandDecoder extends LengthFieldBasedFrameDecoder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDecoder.class);
    private static final int MAX_FRAME_LENGTH = Integer.MAX_VALUE;

    public CommandDecoder() {
        super(MAX_FRAME_LENGTH, 0, 4);
    }
    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (frame == null) {
                return null;
            }
            //ByteBuffer byteBuffer = frame.nioBuffer();
            //logger.info(byteBuffer.toString());
            return Resp.decode(frame);
        }catch (Exception e) {
            ctx.channel().close();
        }finally {
            if (frame != null) {
                frame.release();
            }
        }
        return null;
    }



}
