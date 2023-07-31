package com.wiqer.redis.command.impl.zset;


import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisZset;
import com.wiqer.redis.datatype.ZsetKey;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import io.netty.channel.ChannelHandlerContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Zrevrange implements Command
{
    private BytesWrapper key;
    private int          start;
    private int          end;

    @Override
    public CommandType type()
    {
        return CommandType.zrevrange;
    }

    @Override
    public void setContent(Resp[] array)
    {
        key = ((BulkString) array[1]).getContent();
        start = Integer.parseInt(((BulkString) array[2]).getContent().toUtf8String());
        end = Integer.parseInt(((BulkString) array[3]).getContent().toUtf8String());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        RedisZset               redisZset = (RedisZset) redisCore.get(key);
        List<ZsetKey> keys      = redisZset.reRange(start, end);
        Resp[] resps = keys.stream().flatMap(key -> {
            Resp[] info = new Resp[2];
            BulkString bulkString0 =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkString0.setContent(key.getKey());
            info[0] = bulkString0;
            BulkString bulkString1 =  RedisBaseData.getRedisDataByType(BulkString.class);
            BytesWrapper bytesWrapper =  RedisBaseData.getRedisDataByType(BytesWrapper.class);
            bytesWrapper.setByteArray(String.valueOf(key.getScore()).getBytes(CHARSET));
            bulkString1.setContent(bytesWrapper);
            info[1] = bulkString1;
            return Stream.of(info);
        }).toArray(Resp[]::new);
        RespArray arrays = RedisBaseData.getRedisDataByType(RespArray.class);
        arrays.setArray(resps);
        ctx.writeAndFlush(arrays).addListener(future -> {
            Arrays.stream(resps).forEach(Resp::recovery);
            arrays.recovery();
        });

    }
}
