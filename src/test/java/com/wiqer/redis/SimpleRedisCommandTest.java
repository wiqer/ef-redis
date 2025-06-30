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
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.datatype.RedisHash;
import com.wiqer.redis.datatype.RedisList;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 简单的Redis命令实现测试类
 * 用于验证各种Redis命令的实现是否正确
 */
public class SimpleRedisCommandTest {

    private RedisCoreImpl redisCore;

    @Before
    public void setUp() {
        redisCore = new RedisCoreImpl();
    }

    @Test
    public void testSetAndGetBasic() {
        // 测试基本的SET和GET命令
        BytesWrapper key = new BytesWrapper("test_key".getBytes());
        BytesWrapper value = new BytesWrapper("test_value".getBytes());
        
        // 直接测试RedisCore的put和get方法
        RedisString stringData = new RedisString();
        stringData.setValue(value);
        redisCore.put(key, stringData);
        
        // 验证数据是否正确存储
        assertTrue(redisCore.exist(key));
        
        RedisData retrievedData = redisCore.get(key);
        assertNotNull(retrievedData);
        assertTrue(retrievedData instanceof RedisString);
        
        RedisString retrievedString = (RedisString) retrievedData;
        assertEquals(value, retrievedString.getValue());
    }

    @Test
    public void testSetWithExpiration() {
        // 测试带过期时间的SET命令
        BytesWrapper key = new BytesWrapper("expire_key".getBytes());
        BytesWrapper value = new BytesWrapper("expire_value".getBytes());
        
        RedisString stringData = new RedisString();
        stringData.setValue(value);
        stringData.setTimeout(System.currentTimeMillis() + 10000); // 10秒后过期
        redisCore.put(key, stringData);
        
        // 验证数据是否正确存储
        assertTrue(redisCore.exist(key));
        
        RedisData retrievedData = redisCore.get(key);
        assertNotNull(retrievedData);
        assertTrue(retrievedData instanceof RedisString);
        
        RedisString retrievedString = (RedisString) retrievedData;
        assertEquals(value, retrievedString.getValue());
        assertTrue(retrievedString.timeout() > System.currentTimeMillis());
    }

    @Test
    public void testHashCommands() {
        // 测试Hash相关命令
        BytesWrapper key = new BytesWrapper("hash_key".getBytes());
        BytesWrapper field1 = new BytesWrapper("field1".getBytes());
        BytesWrapper field2 = new BytesWrapper("field2".getBytes());
        BytesWrapper value1 = new BytesWrapper("value1".getBytes());
        BytesWrapper value2 = new BytesWrapper("value2".getBytes());
        
        // 创建Hash并添加字段
        RedisHash hashData = new RedisHash();
        int result1 = hashData.put(field1, value1);
        int result2 = hashData.put(field2, value2);
        
        // 验证put方法返回值（新字段返回1，已存在字段返回0）
        assertEquals(1, result1);
        assertEquals(1, result2);
        
        // 存储Hash
        redisCore.put(key, hashData);
        
        // 验证数据是否正确存储
        assertTrue(redisCore.exist(key));
        
        RedisData retrievedData = redisCore.get(key);
        assertNotNull(retrievedData);
        assertTrue(retrievedData instanceof RedisHash);
        
        RedisHash retrievedHash = (RedisHash) retrievedData;
        assertEquals(value1, retrievedHash.get(field1));
        assertEquals(value2, retrievedHash.get(field2));
    }

    @Test
    public void testListCommands() {
        // 测试List相关命令
        BytesWrapper key = new BytesWrapper("list_key".getBytes());
        BytesWrapper value1 = new BytesWrapper("value1".getBytes());
        BytesWrapper value2 = new BytesWrapper("value2".getBytes());
        BytesWrapper value3 = new BytesWrapper("value3".getBytes());
        
        // 创建List并添加元素
        RedisList listData = new RedisList();
        listData.lpush(value1, value2);
        listData.rpush(java.util.Arrays.asList(value3));
        
        // 验证List大小
        assertEquals(3, listData.size());
        
        // 存储List
        redisCore.put(key, listData);
        
        // 验证数据是否正确存储
        assertTrue(redisCore.exist(key));
        
        RedisData retrievedData = redisCore.get(key);
        assertNotNull(retrievedData);
        assertTrue(retrievedData instanceof RedisList);
        
        RedisList retrievedList = (RedisList) retrievedData;
        assertEquals(3, retrievedList.size());
        
        // 测试pop操作
        BytesWrapper lpopResult = retrievedList.lpop();
        assertEquals(value2, lpopResult); // lpush是后进先出，所以value2在头部
        
        BytesWrapper rpopResult = retrievedList.rpop();
        assertEquals(value3, rpopResult); // rpush是先进先出，所以value3在尾部
    }

    @Test
    public void testSetCommandParameterParsing() {
        // 测试SET命令参数解析逻辑
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
        
        // 直接调用handle方法测试逻辑
        setCommand.handle(redisCore);
        
        // 验证数据是否正确存储
        BytesWrapper key = new BytesWrapper("test_key".getBytes());
        assertTrue(redisCore.exist(key));
    }

    @Test
    public void testSetWithNX() {
        // 测试SET NX命令逻辑
        Set setCommand = new Set();
        
        // 第一次设置
        Resp[] setArray1 = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(new BytesWrapper("nx_key".getBytes())),
            new BulkString(new BytesWrapper("value1".getBytes())),
            new BulkString(new BytesWrapper("NX".getBytes()))
        };
        
        setCommand.setContent(setArray1);
        setCommand.handle(redisCore);
        
        // 验证第一次设置成功
        BytesWrapper key = new BytesWrapper("nx_key".getBytes());
        assertTrue(redisCore.exist(key));
        
        // 第二次尝试设置同一个键
        Resp[] setArray2 = {
            new BulkString(new BytesWrapper("SET".getBytes())),
            new BulkString(new BytesWrapper("nx_key".getBytes())),
            new BulkString(new BytesWrapper("value2".getBytes())),
            new BulkString(new BytesWrapper("NX".getBytes()))
        };
        
        setCommand.setContent(setArray2);
        setCommand.handle(redisCore);
        
        // 验证第二次设置应该失败（键已存在）
        RedisData retrievedData = redisCore.get(key);
        assertNotNull(retrievedData);
        assertTrue(retrievedData instanceof RedisString);
        
        RedisString retrievedString = (RedisString) retrievedData;
        // 值应该还是第一次设置的值
        assertEquals(new BytesWrapper("value1".getBytes()), retrievedString.getValue());
    }

    @Test
    public void testHashSetCommand() {
        // 测试HSET命令逻辑
        Hset hsetCommand = new Hset();
        
        Resp[] hsetArray = {
            new BulkString(new BytesWrapper("HSET".getBytes())),
            new BulkString(new BytesWrapper("hash_key".getBytes())),
            new BulkString(new BytesWrapper("field1".getBytes())),
            new BulkString(new BytesWrapper("value1".getBytes()))
        };
        
        hsetCommand.setContent(hsetArray);
        hsetCommand.handle(redisCore);
        
        // 验证数据是否正确存储
        BytesWrapper key = new BytesWrapper("hash_key".getBytes());
        assertTrue(redisCore.exist(key));
        
        RedisData retrievedData = redisCore.get(key);
        assertNotNull(retrievedData);
        assertTrue(retrievedData instanceof RedisHash);
        
        RedisHash retrievedHash = (RedisHash) retrievedData;
        assertEquals(new BytesWrapper("value1".getBytes()), 
                    retrievedHash.get(new BytesWrapper("field1".getBytes())));
    }

    @Test
    public void testListPushCommand() {
        // 测试LPUSH命令逻辑
        Lpush lpushCommand = new Lpush();
        
        Resp[] lpushArray = {
            new BulkString(new BytesWrapper("LPUSH".getBytes())),
            new BulkString(new BytesWrapper("list_key".getBytes())),
            new BulkString(new BytesWrapper("value1".getBytes())),
            new BulkString(new BytesWrapper("value2".getBytes()))
        };
        
        lpushCommand.setContent(lpushArray);
        lpushCommand.handle(redisCore);
        
        // 验证数据是否正确存储
        BytesWrapper key = new BytesWrapper("list_key".getBytes());
        assertTrue(redisCore.exist(key));
        
        RedisData retrievedData = redisCore.get(key);
        assertNotNull(retrievedData);
        assertTrue(retrievedData instanceof RedisList);
        
        RedisList retrievedList = (RedisList) retrievedData;
        assertEquals(2, retrievedList.size());
    }

    @Test
    public void testTypeChecking() {
        // 测试类型检查逻辑
        BytesWrapper key = new BytesWrapper("test_key".getBytes());
        
        // 先存储一个String类型
        RedisString stringData = new RedisString();
        stringData.setValue(new BytesWrapper("string_value".getBytes()));
        redisCore.put(key, stringData);
        
        // 验证存储的是String类型
        RedisData retrievedData = redisCore.get(key);
        assertTrue(retrievedData instanceof RedisString);
        assertFalse(retrievedData instanceof RedisHash);
        assertFalse(retrievedData instanceof RedisList);
    }

    @Test
    public void testDataExpiration() {
        // 测试数据过期逻辑
        BytesWrapper key = new BytesWrapper("expire_key".getBytes());
        BytesWrapper value = new BytesWrapper("expire_value".getBytes());
        
        // 设置一个已经过期的数据
        RedisString stringData = new RedisString();
        stringData.setValue(value);
        stringData.setTimeout(System.currentTimeMillis() - 1000); // 1秒前过期
        redisCore.put(key, stringData);
        
        // 验证数据存在但已过期
        assertTrue(redisCore.exist(key));
        
        // 注意：实际的过期清理逻辑需要在RedisCore中实现
        // 这里只是测试数据结构本身
        RedisData retrievedData = redisCore.get(key);
        assertNotNull(retrievedData);
        assertTrue(retrievedData instanceof RedisString);
        
        RedisString retrievedString = (RedisString) retrievedData;
        assertTrue(retrievedString.timeout() < System.currentTimeMillis());
    }
} 