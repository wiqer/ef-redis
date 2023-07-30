package com.wiqer.redis.command.impl;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;

import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import com.wiqer.redis.util.StringUtil;
import io.netty.channel.ChannelHandlerContext;


import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Keys implements Command
{
    String pattern="";
    @Override
    public CommandType type()
    {
        return CommandType.keys;
    }

    @Override
    public void setContent(Resp[] array)
    {
        //需要转译的字符(    [     {    /    ^    -    $     ¦    }    ]    )    ?    *    +    .
        pattern= "."+((BulkString) array[1]).getContent().toUtf8String();

    }


    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore)
    {
        Set<BytesWrapper> keySet= redisCore.keys();

        Resp[] resps = keySet.stream().filter(k->{
            String content=null;
            try {
                 content=k.toUtf8String();
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return Pattern.matches(pattern, content);
        }).flatMap(key -> {
            Resp[] info = new Resp[1];
            BulkString bulkString =  RedisBaseData.getRedisDataByType(BulkString.class);
            bulkString.setContent(key);
            info[0] = bulkString;
            return Stream.of(info);
        }).toArray(Resp[]::new);
        RespArray arrays = RedisBaseData.getRedisDataByType(RespArray.class);
        arrays.setArray(resps);
        ctx.writeAndFlush(arrays);
    }
}