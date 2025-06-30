package com.wiqer.redis.command.impl.string;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import io.netty.channel.ChannelHandlerContext;

public class GetRange implements Command {
    private BytesWrapper key;
    private int start;
    private int end;

    @Override
    public CommandType type() {
        return CommandType.getrange;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
        start = Integer.parseInt(((BulkString) array[2]).getContent().toUtf8String());
        end = Integer.parseInt(((BulkString) array[3]).getContent().toUtf8String());
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            ctx.writeAndFlush(BulkString.NullBulkString);
        } else if (redisData instanceof RedisString) {
            BytesWrapper value = ((RedisString) redisData).getValue();
            byte[] bytes = value.getByteArray();
            
            // 处理负数索引
            int actualStart = start < 0 ? bytes.length + start : start;
            int actualEnd = end < 0 ? bytes.length + end : end;
            
            // 边界检查
            if (actualStart >= bytes.length) {
                ctx.writeAndFlush(new BulkString(new BytesWrapper(new byte[0])));
                return;
            }
            
            if (actualEnd >= bytes.length) {
                actualEnd = bytes.length - 1;
            }
            
            if (actualStart > actualEnd) {
                ctx.writeAndFlush(new BulkString(new BytesWrapper(new byte[0])));
                return;
            }
            
            // 提取子串
            int length = actualEnd - actualStart + 1;
            byte[] result = new byte[length];
            System.arraycopy(bytes, actualStart, result, 0, length);
            
            ctx.writeAndFlush(new BulkString(new BytesWrapper(result)));
        } else {
            throw new UnsupportedOperationException();
        }
    }
} 