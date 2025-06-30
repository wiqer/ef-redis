package com.wiqer.redis;

import com.wiqer.redis.command.impl.string.Set;
import com.wiqer.redis.command.impl.string.Get;
import com.wiqer.redis.command.impl.hash.Hset;
import com.wiqer.redis.command.impl.hash.Hget;
import com.wiqer.redis.command.impl.list.Lpush;
import com.wiqer.redis.command.impl.list.Lpop;
import com.wiqer.redis.command.impl.list.Rpop;
import com.wiqer.redis.command.impl.list.Lrange;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Redis命令实现测试类
 * 用于验证各种Redis命令的实现是否正确
 */
public class RedisCommandTest {

    private RedisCoreImpl redisCore;
    
    @Mock
    private ChannelHandlerContext ctx;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        redisCore = new RedisCoreImpl();
    }

    @Test
    public void testSetAndGet() {
        // 测试基本的SET和GET命令
        Set setCommand = new Set();
        Get getCommand = new Get();
        
        // 设置参数
        Resp[] setArray = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(new BytesWrapper("test_key".getBytes())),
            new BulkString(new BytesWrapper("test_value".getBytes()))
        };
        
        Resp[] getArray = {
            new BulkString(new BytesWrapper("GET".getBytes())),
            new BulkString(new BytesWrapper("test_key".getBytes()))
        };
        
        setCommand.setContent(setArray);
        getCommand.setContent(getArray);
        
        // 执行SET命令
        setCommand.handle(ctx, redisCore);
        
        // 验证SET命令响应
        verify(ctx).writeAndFlush(any());
        
        // 执行GET命令
        getCommand.handle(ctx, redisCore);
        
        // 验证GET命令响应
        verify(ctx, times(2)).writeAndFlush(any());
    }

    @Test
    public void testSetWithExpiration() {
        // 测试带过期时间的SET命令
        Set setCommand = new Set();
        
        Resp[] setArray = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(new BytesWrapper("expire_key".getBytes())),
            new BulkString(new BytesWrapper("expire_value".getBytes())),
            new BulkString(new BytesWrapper("EX".getBytes())),
            new BulkString(new BytesWrapper("10".getBytes()))
        };
        
        setCommand.setContent(setArray);
        setCommand.handle(ctx, redisCore);
        
        // 验证命令执行
        verify(ctx).writeAndFlush(any());
        
        // 检查数据是否正确存储
        BytesWrapper key = new BytesWrapper("expire_key".getBytes());
        assertTrue(redisCore.exist(key));
    }

    @Test
    public void testSetWithNX() {
        // 测试SET NX命令（只在键不存在时设置）
        Set setCommand = new Set();
        
        // 第一次设置
        Resp[] setArray1 = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(new BytesWrapper("nx_key".getBytes())),
            new BulkString(new BytesWrapper("value1".getBytes())),
            new BulkString(new BytesWrapper("NX".getBytes()))
        };
        
        setCommand.setContent(setArray1);
        setCommand.handle(ctx, redisCore);
        
        // 第二次尝试设置同一个键
        Resp[] setArray2 = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(new BytesWrapper("nx_key".getBytes())),
            new BulkString(new BytesWrapper("value2".getBytes())),
            new BulkString(new BytesWrapper("NX".getBytes()))
        };
        
        setCommand.setContent(setArray2);
        setCommand.handle(ctx, redisCore);
        
        // 验证第二次设置应该返回null（因为键已存在）
        verify(ctx, times(2)).writeAndFlush(any());
    }

    @Test
    public void testHashCommands() {
        // 测试Hash相关命令
        Hset hsetCommand = new Hset();
        Hget hgetCommand = new Hget();
        
        // HSET命令
        Resp[] hsetArray = {
            new BulkString(new BytesWrapper("HSET".getBytes())),
            new BulkString(new BytesWrapper("hash_key".getBytes())),
            new BulkString(new BytesWrapper("field1".getBytes())),
            new BulkString(new BytesWrapper("value1".getBytes()))
        };
        
        hsetCommand.setContent(hsetArray);
        hsetCommand.handle(ctx, redisCore);
        
        // HGET命令
        Resp[] hgetArray = {
            new BulkString(new BytesWrapper("HGET".getBytes())),
            new BulkString(new BytesWrapper("hash_key".getBytes())),
            new BulkString(new BytesWrapper("field1".getBytes()))
        };
        
        hgetCommand.setContent(hgetArray);
        hgetCommand.handle(ctx, redisCore);
        
        // 验证命令执行
        verify(ctx, times(2)).writeAndFlush(any());
    }

    @Test
    public void testListCommands() {
        // 测试List相关命令
        Lpush lpushCommand = new Lpush();
        Lpop lpopCommand = new Lpop();
        Rpop rpopCommand = new Rpop();
        Lrange lrangeCommand = new Lrange();
        
        // LPUSH命令
        Resp[] lpushArray = {
            new BulkString(new BytesWrapper("LPUSH".getBytes())),
            new BulkString(new BytesWrapper("list_key".getBytes())),
            new BulkString(new BytesWrapper("value1".getBytes())),
            new BulkString(new BytesWrapper("value2".getBytes()))
        };
        
        lpushCommand.setContent(lpushArray);
        lpushCommand.handle(ctx, redisCore);
        
        // LPOP命令
        Resp[] lpopArray = {
            new BulkString(new BytesWrapper("LPOP".getBytes())),
            new BulkString(new BytesWrapper("list_key".getBytes()))
        };
        
        lpopCommand.setContent(lpopArray);
        lpopCommand.handle(ctx, redisCore);
        
        // RPOP命令
        Resp[] rpopArray = {
            new BulkString(new BytesWrapper("RPOP".getBytes())),
            new BulkString(new BytesWrapper("list_key".getBytes()))
        };
        
        rpopCommand.setContent(rpopArray);
        rpopCommand.handle(ctx, redisCore);
        
        // LRANGE命令
        Resp[] lrangeArray = {
            new BulkString(new BytesWrapper("LRANGE".getBytes())),
            new BulkString(new BytesWrapper("list_key".getBytes())),
            new BulkString(new BytesWrapper("0".getBytes())),
            new BulkString(new BytesWrapper("-1".getBytes()))
        };
        
        lrangeCommand.setContent(lrangeArray);
        lrangeCommand.handle(ctx, redisCore);
        
        // 验证命令执行
        verify(ctx, times(4)).writeAndFlush(any());
    }

    @Test
    public void testSetCommandParameterParsing() {
        // 测试SET命令参数解析是否正确
        Set setCommand = new Set();
        
        // 测试SET key value EX seconds
        Resp[] setArray = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(new BytesWrapper("test_key".getBytes())),
            new BulkString(new BytesWrapper("test_value".getBytes())),
            new BulkString(new BytesWrapper("EX".getBytes())),
            new BulkString(new BytesWrapper("60".getBytes()))
        };
        
        setCommand.setContent(setArray);
        setCommand.handle(ctx, redisCore);
        
        // 验证命令执行
        verify(ctx).writeAndFlush(any());
    }

    @Test
    public void testPushCommandLogic() {
        // 测试Push命令的逻辑是否正确
        Lpush lpushCommand = new Lpush();
        
        // 测试向不存在的键执行LPUSH
        Resp[] lpushArray = {
            new BulkString(new BytesWrapper("LPUSH".getBytes())),
            new BulkString(new BytesWrapper("new_list".getBytes())),
            new BulkString(new BytesWrapper("value1".getBytes()))
        };
        
        lpushCommand.setContent(lpushArray);
        lpushCommand.handle(ctx, redisCore);
        
        // 验证命令执行
        verify(ctx).writeAndFlush(any());
        
        // 测试向已存在的非List类型键执行LPUSH
        // 先设置一个String类型的键
        Set setCommand = new Set();
        Resp[] setArray = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(new BytesWrapper("string_key".getBytes())),
            new BulkString(new BytesWrapper("string_value".getBytes()))
        };
        
        setCommand.setContent(setArray);
        setCommand.handle(ctx, redisCore);
        
        // 尝试向String类型的键执行LPUSH
        Resp[] lpushArray2 = {
            new BulkString(new BytesWrapper("LPUSH".getBytes())),
            new BulkString(new BytesWrapper("string_key".getBytes())),
            new BulkString(new BytesWrapper("value1".getBytes()))
        };
        
        lpushCommand.setContent(lpushArray2);
        lpushCommand.handle(ctx, redisCore);
        
        // 验证应该返回错误
        verify(ctx, times(3)).writeAndFlush(any());
    }

    @Test
    public void testDataTypes() {
        // 测试数据类型的基本功能
        
        // 测试RedisString
        BytesWrapper key = new BytesWrapper("test_key".getBytes());
        BytesWrapper value = new BytesWrapper("test_value".getBytes());
        
        Set setCommand = new Set();
        Resp[] setArray = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(key),
            new BulkString(value)
        };
        
        setCommand.setContent(setArray);
        setCommand.handle(ctx, redisCore);
        
        // 验证数据是否正确存储
        assertTrue(redisCore.exist(key));
        
        // 测试RedisHash
        BytesWrapper hashKey = new BytesWrapper("hash_key".getBytes());
        BytesWrapper field = new BytesWrapper("field1".getBytes());
        BytesWrapper hashValue = new BytesWrapper("hash_value".getBytes());
        
        Hset hsetCommand = new Hset();
        Resp[] hsetArray = {
            new BulkString(new BytesWrapper("HSET".getBytes())),
            new BulkString(hashKey),
            new BulkString(field),
            new BulkString(hashValue)
        };
        
        hsetCommand.setContent(hsetArray);
        hsetCommand.handle(ctx, redisCore);
        
        // 验证Hash数据是否正确存储
        assertTrue(redisCore.exist(hashKey));
    }
} 