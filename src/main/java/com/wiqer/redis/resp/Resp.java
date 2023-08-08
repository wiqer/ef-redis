package com.wiqer.redis.resp;

import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisBaseData;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Administrator
 */
public interface Resp extends RedisBaseData {

    static void write(Resp resp, ByteBuf buffer) {

        if (resp instanceof SimpleString) {
            buffer.writeByte(RespType.STATUS.getCode());
            String content = ((SimpleString) resp).getContent();
            char[] charArray = content.toCharArray();
            for (char each : charArray) {
                buffer.writeByte((byte) each);
            }
            buffer.writeByte(RespType.R.getCode());
            buffer.writeByte(RespType.N.getCode());
        } else if (resp instanceof Errors) {
            buffer.writeByte(RespType.ERROR.getCode());
            String content = ((Errors) resp).getContent();
            char[] charArray = content.toCharArray();
            for (char each : charArray) {
                buffer.writeByte((byte) each);
            }
            buffer.writeByte(RespType.R.getCode());
            buffer.writeByte(RespType.N.getCode());
        } else if (resp instanceof RespInt) {
            buffer.writeByte(RespType.INTEGER.getCode());
            String content = String.valueOf(((RespInt) resp).getValue());
            char[] charArray = content.toCharArray();
            for (char each : charArray) {
                buffer.writeByte((byte) each);
            }
            buffer.writeByte(RespType.R.getCode());
            buffer.writeByte(RespType.N.getCode());
        } else if (resp instanceof BulkString) {
            buffer.writeByte(RespType.BULK.getCode());
            BytesWrapper content = ((BulkString) resp).getContent();
            if (content == null) {
                buffer.writeByte(RespType.ERROR.getCode());
                buffer.writeByte(RespType.ONE.getCode());
                buffer.writeByte(RespType.R.getCode());
                buffer.writeByte(RespType.N.getCode());
            } else if (content.getByteArray().length == 0) {
                buffer.writeByte(RespType.ZERO.getCode());
                buffer.writeByte(RespType.R.getCode());
                buffer.writeByte(RespType.N.getCode());
                buffer.writeByte(RespType.R.getCode());
                buffer.writeByte(RespType.N.getCode());
            } else {
                String length = String.valueOf(content.getByteArray().length);
                char[] charArray = length.toCharArray();
                for (char each : charArray) {
                    buffer.writeByte((byte) each);
                }
                buffer.writeByte(RespType.R.getCode());
                buffer.writeByte(RespType.N.getCode());
                buffer.writeBytes(content.getByteArray());
                buffer.writeByte(RespType.R.getCode());
                buffer.writeByte(RespType.N.getCode());
            }
        } else if (resp instanceof RespArray) {
            buffer.writeByte(RespType.MULTYBULK.getCode());
            Resp[] array = ((RespArray) resp).getArray();
            String length = String.valueOf(array.length);
            char[] charArray = length.toCharArray();
            for (char each : charArray) {
                buffer.writeByte((byte) each);
            }
            buffer.writeByte(RespType.R.getCode());
            buffer.writeByte(RespType.N.getCode());
            for (Resp each : array) {
                write(each, buffer);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * 无法解码压测客户端
     *
     * @param buffer
     * @return
     */
    static Resp decode(ByteBuf buffer) {

        if (buffer.readableBytes() <= 0) {
            throw new IllegalStateException("没有读取到完整的命令");
        }
        char c = (char) buffer.readByte();
        if (c == RespType.STATUS.getCode()) {
            Long threadId = Thread.currentThread().getId();
            SimpleString simpleString =  RedisBaseData.getRedisDataByTypeAndThreadId(SimpleString.class, threadId);
            simpleString.setCreatedThreadId(threadId);
            simpleString.setContent(getString(buffer, threadId));
            return simpleString;
        } else if (c == RespType.ERROR.getCode()) {
            Long threadId = Thread.currentThread().getId();
            Errors err =  RedisBaseData.getRedisDataByTypeAndThreadId(Errors.class, threadId);
            err.setCreatedThreadId(threadId);
            err.setContent(getString(buffer, threadId));
            return err;
        } else if (c == RespType.INTEGER.getCode()) {
            int value = getNumber(buffer);
            Long threadId = Thread.currentThread().getId();
            RespInt respInt =  RedisBaseData.getRedisDataByTypeAndThreadId(RespInt.class, threadId);
            respInt.setCreatedThreadId(threadId);
            respInt.setValue(value);
            return respInt;
        } else if (c == RespType.BULK.getCode()) {
            int length = getNumber(buffer);
            if (buffer.readableBytes() < length + 2) {
                throw new IllegalStateException("没有读取到完整的命令");
            }
            byte[] content;
            if (length == -1) {
                content = null;
            } else {
                content = new byte[length];
                buffer.readBytes(content);
            }
            if (buffer.readByte() != RespType.R.getCode() || buffer.readByte() != RespType.N.getCode()) {
                throw new IllegalStateException("没有读取到完整的命令");
            }
            Long threadId = Thread.currentThread().getId();
            BulkString bulkString =  RedisBaseData.getRedisDataByTypeAndThreadId(BulkString.class, threadId);
            bulkString.setCreatedThreadId(threadId);
            BytesWrapper bytesWrapper =  RedisBaseData.getRedisDataByTypeAndThreadId(BytesWrapper.class, threadId);
            bytesWrapper.setCreatedThreadId(threadId);
            bytesWrapper.setByteArray(content);
            bulkString.setContent(bytesWrapper);
            return bulkString;
        } else if (c == RespType.MULTYBULK.getCode()) {
            int numOfElement = getNumber(buffer);
            Resp[] array = new Resp[numOfElement];
            for (int i = 0; i < numOfElement; i++) {
                array[i] = decode(buffer);
            }
            Long threadId = Thread.currentThread().getId();
            RespArray respArray =  RedisBaseData.getRedisDataByTypeAndThreadId(RespArray.class, threadId);
            respArray.setArray(array);
            respArray.setCreatedThreadId(threadId);
            return respArray;
        } else {
            /**
             * A~Z
             */
            if (c > 64 && c < 91) {
                Long threadId = Thread.currentThread().getId();
                SimpleString simpleString =  RedisBaseData.getRedisDataByTypeAndThreadId(SimpleString.class, threadId);
                simpleString.setContent(c + getString(buffer, threadId));
                simpleString.setCreatedThreadId(Thread.currentThread().getId());
                return  simpleString;
            } else {
                return decode(buffer);
            }

            //throw new IllegalArgumentException("意外地命令");
        }
    }

    static int getNumber(ByteBuf buffer) {
        char t;
        t = (char) buffer.readByte();
        boolean positive = true;
        int value = 0;
        // 错误（Errors）： 响应的首字节是 "-"
        if (t == RespType.ERROR.getCode()) {
            positive = false;
        } else {
            value = t - RespType.ZERO.getCode();
        }
        while (buffer.readableBytes() > 0 && (t = (char) buffer.readByte()) != RespType.R.getCode()) {
            value = value * 10 + (t - RespType.ZERO.getCode());
        }
        if (buffer.readableBytes() == 0 || buffer.readByte() != RespType.N.getCode()) {
            throw new IllegalStateException("没有读取到完整的命令");
        }
        if (!positive) {
            value = -value;
        }
        return value;
    }
    static HashMap<Long, StringBuilder> LOCAL_THREAD_GET_STRING_BUILDER_MAP = new HashMap<>();
    static String getString(ByteBuf buffer, Long threadId) {
        char c;
        StringBuilder builder = LOCAL_THREAD_GET_STRING_BUILDER_MAP.computeIfAbsent(threadId, v ->  new StringBuilder());
        builder.setLength(0);
        while (buffer.readableBytes() > 0 && (c = (char) buffer.readByte()) != RespType.R.getCode()) {
            builder.append(c);
        }
        if (buffer.readableBytes() == 0 || buffer.readByte() != RespType.N.getCode()) {
            throw new IllegalStateException("没有读取到完整的命令");
        }
        return builder.toString();
    }
}
