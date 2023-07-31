package com.wiqer.redis.command.impl;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import com.wiqer.redis.util.TRACEID;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

public class Config implements Command
{
    private String param;

    @Override
    public CommandType type()
    {
        return CommandType.config;
    }

    @Override
    public void setContent(Resp[] array)
    {
        if (array.length != 3)
        {
            throw new IllegalStateException();
        }
        if (((BulkString) array[1]).getContent().toUtf8String().equals("get") == false)
        {
            throw new IllegalStateException();
        }
        param = ((BulkString) array[2]).getContent().toUtf8String();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        if (param.equals("*") || param.equals("databases"))
        {
            List<BulkString> list = new ArrayList<>();
            BulkString bulkString0 =  RedisBaseData.getRedisDataByType(BulkString.class);
            BytesWrapper bytesWrapper =  RedisBaseData.getRedisDataByType(BytesWrapper.class);
            bytesWrapper.setByteArray("databases".getBytes(CHARSET));
            bulkString0.setContent(bytesWrapper);
            list.add(bulkString0);
            BulkString bulkString1 =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkString1.setContent(BytesWrapper.ONE);
            list.add(bulkString1);
            Resp[]    array  = list.toArray(new Resp[list.size()]);
            RespArray arrays = RedisBaseData.getRedisDataByType(RespArray.class);
            arrays.setArray(array);
            ctx.writeAndFlush(arrays).addListener(future -> {
                bytesWrapper.recovery();
                bulkString0.recovery();
                bulkString1.recovery();
                arrays.recovery();
            });
        }
        else
        {
            String traceId = TRACEID.currentTraceId();
            LOGGER.debug("traceId:"+traceId+" 不识别的Config命令模式:"+param );
            ctx.channel().close();
        }
    }
}
