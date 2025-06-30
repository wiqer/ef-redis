package com.wiqer.redis.command.impl.string;

import com.wiqer.redis.RedisCore;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespInt;
import io.netty.channel.ChannelHandlerContext;

public class Append implements WriteCommand {
    private BytesWrapper key;
    private BytesWrapper value;

    @Override
    public CommandType type() {
        return CommandType.append;
    }

    @Override
    public void setContent(Resp[] array) {
        key = ((BulkString) array[1]).getContent();
        value = ((BulkString) array[2]).getContent();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            // 如果key不存在，创建一个新的字符串
            RedisString redisString = new RedisString();
            redisString.setValue(value);
            redisCore.put(key, redisString);
            ctx.writeAndFlush(new RespInt(value.getByteArray().length));
        } else if (redisData instanceof RedisString) {
            // 如果key存在且是字符串类型，追加内容
            BytesWrapper currentValue = ((RedisString) redisData).getValue();
            byte[] currentBytes = currentValue.getByteArray();
            byte[] appendBytes = value.getByteArray();
            
            // 创建新的字节数组
            byte[] newBytes = new byte[currentBytes.length + appendBytes.length];
            System.arraycopy(currentBytes, 0, newBytes, 0, currentBytes.length);
            System.arraycopy(appendBytes, 0, newBytes, currentBytes.length, appendBytes.length);
            
            // 更新值
            ((RedisString) redisData).setValue(new BytesWrapper(newBytes));
            redisCore.put(key, redisData);
            ctx.writeAndFlush(new RespInt(newBytes.length));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void handle(RedisCore redisCore) {
        RedisData redisData = redisCore.get(key);
        if (redisData == null) {
            RedisString redisString = new RedisString();
            redisString.setValue(value);
            redisCore.put(key, redisString);
        } else if (redisData instanceof RedisString) {
            BytesWrapper currentValue = ((RedisString) redisData).getValue();
            byte[] currentBytes = currentValue.getByteArray();
            byte[] appendBytes = value.getByteArray();
            
            byte[] newBytes = new byte[currentBytes.length + appendBytes.length];
            System.arraycopy(currentBytes, 0, newBytes, 0, currentBytes.length);
            System.arraycopy(appendBytes, 0, newBytes, currentBytes.length, appendBytes.length);
            
            ((RedisString) redisData).setValue(new BytesWrapper(newBytes));
            redisCore.put(key, redisData);
        }
    }
} 