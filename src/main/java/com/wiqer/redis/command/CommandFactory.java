package com.wiqer.redis.command;


import com.wiqer.redis.MyRedisServer;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import com.wiqer.redis.resp.SimpleString;
import com.wiqer.redis.util.TRACEID;
import lombok.extern.slf4j.Slf4j;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class CommandFactory {
    static Map<String, Supplier<Command>> map = new HashMap<>(CommandType.values().length, 0.5F);

    static {
        for (CommandType each : CommandType.values()) {
            map.put(each.name(), each.getSupplier());
        }
    }

    public static Command from(RespArray arrays) {
        Resp[] array = arrays.getArray();
        String commandName = ((BulkString) array[0]).getContent().toUtf8String().toLowerCase();
        Supplier<Command> supplier = map.get(commandName);
        if (supplier == null) {
            log.debug("traceId:" + TRACEID.currentTraceId() + " 不支持的命令：" + commandName);
            System.out.println("不支持的命令：" + commandName);
            return null;
        } else {
            try {
                Command command = supplier.get();
                command.setContent(array);
                return command;
            } catch (Throwable e) {
                log.debug("traceId:" + TRACEID.currentTraceId() + " 不支持的命令：{},数据读取异常" + commandName);
                e.printStackTrace();
                return null;
            }
        }
    }

    public static Command from(SimpleString string) {
        String commandName = string.getContent().toLowerCase();
        Supplier<Command> supplier = map.get(commandName);
        if (supplier == null) {
            log.debug("traceId:" + TRACEID.currentTraceId() + " 不支持的命令：" + commandName);
            System.out.println("不支持的命令：" + commandName);
            return null;
        } else {
            try {
                return supplier.get();
            } catch (Throwable e) {
                log.debug("traceId:" + TRACEID.currentTraceId() + " 不支持的命令：{},数据读取异常" + commandName);
                e.printStackTrace();
                return null;
            }
        }
    }
}
