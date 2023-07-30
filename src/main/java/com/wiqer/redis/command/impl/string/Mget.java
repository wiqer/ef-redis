package com.wiqer.redis.command.impl.string;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mget implements Command
{
    private List<BytesWrapper> keys;

    @Override
    public CommandType type()
    {
        return CommandType.mget;
    }

    @Override
    public void setContent(Resp[] array)
    {
        keys = Stream.of(array).skip(1).map(resp -> ((BulkString) resp).getContent()).collect(Collectors.toList());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        LinkedList<BytesWrapper> linkedList= new LinkedList();
        keys.forEach(key -> {
            RedisData redisData = redisCore.get(key);
            if (redisData == null)
            {
            }
            else if (redisData instanceof RedisString)
            {
                linkedList.add(((RedisString) redisData).getValue()) ;
            }
            else
            {
                throw new UnsupportedOperationException();
            }
        });
        RespArray arrays = RedisBaseData.getRedisDataByType(RespArray.class);
        arrays.setArray(linkedList.stream().map(bytesWrapper -> {
            BulkString bulkString =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkString.setContent(bytesWrapper);
            return  bulkString;
        }).toArray(Resp[]::new));
        ctx.writeAndFlush(arrays);
    }

}
