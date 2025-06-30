package com.wiqer.redis;

import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisString;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 隔离测试，专门测试基本的数据存储和检索功能
 */
public class IsolatedTest {

    private RedisCoreImpl redisCore;

    @Before
    public void setUp() {
        redisCore = new RedisCoreImpl();
    }

    @Test
    public void testSimpleStorage() {
        System.out.println("=== 简单存储测试 ===");
        
        // 创建key和value
        BytesWrapper key = new BytesWrapper("simple_key".getBytes());
        BytesWrapper value = new BytesWrapper("simple_value".getBytes());
        
        System.out.println("Key: " + key);
        System.out.println("Value: " + value);
        
        // 创建RedisString
        RedisString stringData = new RedisString();
        stringData.setValue(value);
        stringData.setTimeout(-1);
        
        System.out.println("StringData timeout: " + stringData.timeout());
        
        // 存储数据
        System.out.println("存储数据...");
        redisCore.put(key, stringData);
        
        // 立即检查数据是否存在
        System.out.println("检查数据是否存在...");
        boolean exists = redisCore.exist(key);
        System.out.println("Key exists: " + exists);
        
        // 获取数据
        System.out.println("获取数据...");
        RedisData retrievedData = redisCore.get(key);
        System.out.println("Retrieved data: " + retrievedData);
        
        if (retrievedData != null) {
            System.out.println("Data type: " + retrievedData.getClass().getSimpleName());
            if (retrievedData instanceof RedisString) {
                RedisString retrievedString = (RedisString) retrievedData;
                System.out.println("String value: " + retrievedString.getValue());
                System.out.println("String timeout: " + retrievedString.timeout());
            }
        }
        
        // 断言
        assertTrue("Key should exist", exists);
        assertNotNull("Retrieved data should not be null", retrievedData);
    }

    @Test
    public void testMultipleKeys() {
        System.out.println("=== 多键存储测试 ===");
        
        // 存储多个键值对
        for (int i = 0; i < 5; i++) {
            BytesWrapper key = new BytesWrapper(("key" + i).getBytes());
            BytesWrapper value = new BytesWrapper(("value" + i).getBytes());
            
            RedisString stringData = new RedisString();
            stringData.setValue(value);
            stringData.setTimeout(-1);
            
            redisCore.put(key, stringData);
            
            System.out.println("Stored key" + i + ": " + redisCore.exist(key));
        }
        
        // 检查所有键
        for (int i = 0; i < 5; i++) {
            BytesWrapper key = new BytesWrapper(("key" + i).getBytes());
            boolean exists = redisCore.exist(key);
            System.out.println("Key" + i + " exists: " + exists);
            assertTrue("Key" + i + " should exist", exists);
        }
    }

    @Test
    public void testConcurrentHashMapBehavior() {
        System.out.println("=== ConcurrentHashMap行为测试 ===");
        
        // 测试ConcurrentHashMap的基本行为
        BytesWrapper key1 = new BytesWrapper("test_key".getBytes());
        BytesWrapper key2 = new BytesWrapper("test_key".getBytes());
        
        System.out.println("Key1 equals Key2: " + key1.equals(key2));
        System.out.println("Key1 hashCode: " + key1.hashCode());
        System.out.println("Key2 hashCode: " + key2.hashCode());
        
        // 存储使用key1
        RedisString stringData = new RedisString();
        stringData.setValue(new BytesWrapper("test_value".getBytes()));
        redisCore.put(key1, stringData);
        
        // 使用key2检查
        boolean exists1 = redisCore.exist(key1);
        boolean exists2 = redisCore.exist(key2);
        
        System.out.println("Exists with key1: " + exists1);
        System.out.println("Exists with key2: " + exists2);
        
        assertTrue("Should exist with key1", exists1);
        assertTrue("Should exist with key2 (same content)", exists2);
    }

    @Test
    public void testDataRetrieval() {
        System.out.println("=== 数据检索测试 ===");
        
        BytesWrapper key = new BytesWrapper("retrieval_key".getBytes());
        BytesWrapper value = new BytesWrapper("retrieval_value".getBytes());
        
        // 存储数据
        RedisString stringData = new RedisString();
        stringData.setValue(value);
        stringData.setTimeout(-1);
        redisCore.put(key, stringData);
        
        // 多次检索数据
        for (int i = 0; i < 3; i++) {
            System.out.println("Retrieval attempt " + (i + 1));
            
            boolean exists = redisCore.exist(key);
            System.out.println("Exists: " + exists);
            
            RedisData data = redisCore.get(key);
            System.out.println("Data: " + data);
            
            assertTrue("Should exist on attempt " + (i + 1), exists);
            assertNotNull("Data should not be null on attempt " + (i + 1), data);
        }
    }
} 