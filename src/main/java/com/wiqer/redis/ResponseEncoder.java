package com.wiqer.redis;


import com.wiqer.redis.resp.Resp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

//WriteProcessor<Resp>
public class ResponseEncoder extends MessageToByteEncoder<Resp> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Resp resp, ByteBuf byteBuf) throws Exception {
        try {
            Resp.write(resp, byteBuf);//msg.encode();
            byteBuf.writeBytes(byteBuf);
            // log.info(byteBuffer.toString());
        } catch (Exception e) {
            channelHandlerContext.close();
        }
    }

}
