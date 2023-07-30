package com.wiqer.redis.command.impl.set;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.stream.Collectors;

public class Scan implements Command
{
    @Override
    public CommandType type()
    {
        return CommandType.scan;
    }

    @Override
    public void setContent(Resp[] array)
    {
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        Resp[]     array       = new Resp[2];
        BulkString bulkString =  RedisBaseData.getRedisDataByType(BulkString.class);
        bulkString.setContent(BytesWrapper.ZERO);
        array[0] = bulkString;
        List<BulkString> collect = redisCore.keys().stream().map(keyName -> {
            BulkString bulkStringSub =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkStringSub.setContent(keyName);
            return bulkStringSub;
        }).collect(Collectors.toList());
        RespArray arrays1 = RedisBaseData.getRedisDataByType(RespArray.class);
        arrays1.setArray(collect.toArray(new Resp[collect.size()]));
        array[1] = arrays1;
        RespArray arrays = RedisBaseData.getRedisDataByType(RespArray.class);
        arrays.setArray(array);
        ctx.writeAndFlush(arrays);
    }
}
