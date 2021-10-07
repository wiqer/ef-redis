package com.wiqer.redis.command;


import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import com.wiqer.redis.util.TRACEID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CommandFactory
{
    private static final Logger                         LOGGER = LoggerFactory.getLogger(CommandFactory.class);
    static               Map<String, Supplier<Command>> map    = new HashMap<>();

    static
    {
        for (CommandType each : CommandType.values())
        {
            map.put(each.name(), each.getSupplier());
        }
    }

    public static Command from(RespArray arrays)
    {
        Resp[]            array       = arrays.getArray();
        String            commandName = ((BulkString) array[0]).getContent().toUtf8String().toLowerCase();
        Supplier<Command> supplier    = map.get(commandName);
        if (supplier == null)
        {
            LOGGER.debug("traceId:{} 不支持的命令：{}", TRACEID.currentTraceId(), commandName);
            System.out.println("不支持的命令：" + commandName);
            return null;
        }
        else
        {
            try
            {
                Command command = supplier.get();
                command.setContent(array);
                return command;
            }
            catch (Throwable e)
            {
                LOGGER.debug("traceId:{} 不支持的命令：{},数据读取异常", TRACEID.currentTraceId(), commandName);
                e.printStackTrace();
                return null;
            }
        }
    }

}
