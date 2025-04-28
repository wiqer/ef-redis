package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import com.wiqer.redis.util.TRACEID;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Config implements Command {
    private String param;

    @Override
    public CommandType type() {
        return CommandType.config;
    }

    @Override
    public void setContent(Resp[] array) {
        if (array.length != 3) {
            throw new IllegalStateException();
        }
        if (!((BulkString) array[1]).getContent().toUtf8String().equals("get")) {
            throw new IllegalStateException();
        }
        param = ((BulkString) array[2]).getContent().toUtf8String();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        if (param.equals("*") || param.equals("databases")) {
            List<BulkString> list = new ArrayList<>();
            list.add(new BulkString(new BytesWrapper("databases".getBytes(CHARSET))));
            list.add(new BulkString(new BytesWrapper("1".getBytes(CHARSET))));
            Resp[] array = list.toArray(new Resp[list.size()]);
            RespArray arrays = new RespArray(array);
            ctx.writeAndFlush(arrays);
        } else {
            String traceId = TRACEID.currentTraceId();
            log.debug("traceId:" + traceId + " 不识别的Config命令模式:" + param);
            ctx.channel().close();
        }
    }
}
