package com.wiqer.redis;

import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.datatype.RedisHash;
import com.wiqer.redis.datatype.RedisList;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 调试Redis实现问题的测试类
 */
public class DebugRedisTest {

    private RedisCoreImpl redisCore;

    @Before
    public void setUp() {
        redisCore = new RedisCoreImpl();
    }

    @Test
    public void testBasicPutAndGet() {
        // 测试基本的put和get操作
        BytesWrapper key = new BytesWrapper("test_key".getBytes());
        BytesWrapper value = new BytesWrapper("test_value".getBytes());
        
        System.out.println("测试基本put和get操作");
        System.out.println("Key: " + key);
        System.out.println("Value: " + value);
        
        // 创建RedisString
        RedisString stringData = new RedisString();
        stringData.setValue(value);
        stringData.setTimeout(-1); // 设置不过期
        
        System.out.println("RedisString timeout: " + stringData.timeout());
        
        // 存储数据
        redisCore.put(key, stringData);
        
        // 检查数据是否存在
        boolean exists = redisCore.exist(key);
        System.out.println("Key exists: " + exists);
        
        // 获取数据
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
        assertTrue("Retrieved data should be RedisString", retrievedData instanceof RedisString);
        
        RedisString retrievedString = (RedisString) retrievedData;
        assertEquals("Value should match", value, retrievedString.getValue());
    }

    @Test
    public void testSetCommandImplementation() {
        // 测试SET命令的实现
        System.out.println("\n测试SET命令实现");
        
        // 模拟SET命令的参数解析
        BytesWrapper key = new BytesWrapper("set_test_key".getBytes());
        BytesWrapper value = new BytesWrapper("set_test_value".getBytes());
        
        // 创建RedisString（模拟SET命令的行为）
        RedisString stringData = new RedisString();
        stringData.setValue(value);
        stringData.setTimeout(-1); // 默认不过期
        
        System.out.println("Setting key: " + key);
        System.out.println("Setting value: " + value);
        System.out.println("StringData timeout: " + stringData.timeout());
        
        // 存储数据
        redisCore.put(key, stringData);
        
        // 验证存储
        boolean exists = redisCore.exist(key);
        System.out.println("Key exists after put: " + exists);
        
        RedisData retrievedData = redisCore.get(key);
        System.out.println("Retrieved data after put: " + retrievedData);
        
        assertTrue("Key should exist after put", exists);
        assertNotNull("Retrieved data should not be null", retrievedData);
    }

    @Test
    public void testHashImplementation() {
        // 测试Hash实现
        System.out.println("\n测试Hash实现");
        
        BytesWrapper key = new BytesWrapper("hash_test_key".getBytes());
        BytesWrapper field = new BytesWrapper("field1".getBytes());
        BytesWrapper value = new BytesWrapper("hash_value1".getBytes());
        
        // 创建Hash
        RedisHash hashData = new RedisHash();
        int result = hashData.put(field, value);
        
        System.out.println("Hash put result: " + result);
        System.out.println("Hash size: " + hashData.getMap().size());
        
        // 存储Hash
        redisCore.put(key, hashData);
        
        // 验证存储
        boolean exists = redisCore.exist(key);
        System.out.println("Hash key exists: " + exists);
        
        RedisData retrievedData = redisCore.get(key);
        System.out.println("Retrieved hash data: " + retrievedData);
        
        if (retrievedData instanceof RedisHash) {
            RedisHash retrievedHash = (RedisHash) retrievedData;
            System.out.println("Retrieved hash size: " + retrievedHash.getMap().size());
            System.out.println("Field value: " + retrievedHash.get(field));
        }
        
        assertTrue("Hash key should exist", exists);
        assertNotNull("Retrieved hash data should not be null", retrievedData);
        assertTrue("Retrieved data should be RedisHash", retrievedData instanceof RedisHash);
    }

    @Test
    public void testListImplementation() {
        // 测试List实现
        System.out.println("\n测试List实现");
        
        BytesWrapper key = new BytesWrapper("list_test_key".getBytes());
        BytesWrapper value1 = new BytesWrapper("value1".getBytes());
        BytesWrapper value2 = new BytesWrapper("value2".getBytes());
        
        // 创建List
        RedisList listData = new RedisList();
        listData.lpush(value1, value2);
        
        System.out.println("List size after lpush: " + listData.size());
        
        // 存储List
        redisCore.put(key, listData);
        
        // 验证存储
        boolean exists = redisCore.exist(key);
        System.out.println("List key exists: " + exists);
        
        RedisData retrievedData = redisCore.get(key);
        System.out.println("Retrieved list data: " + retrievedData);
        
        if (retrievedData instanceof RedisList) {
            RedisList retrievedList = (RedisList) retrievedData;
            System.out.println("Retrieved list size: " + retrievedList.size());
        }
        
        assertTrue("List key should exist", exists);
        assertNotNull("Retrieved list data should not be null", retrievedData);
        assertTrue("Retrieved data should be RedisList", retrievedData instanceof RedisList);
    }

    @Test
    public void testExpirationLogic() {
        // 测试过期逻辑
        System.out.println("\n测试过期逻辑");
        
        BytesWrapper key = new BytesWrapper("expire_test_key".getBytes());
        BytesWrapper value = new BytesWrapper("expire_test_value".getBytes());
        
        // 创建已过期的数据
        RedisString stringData = new RedisString();
        stringData.setValue(value);
        stringData.setTimeout(System.currentTimeMillis() - 1000); // 1秒前过期
        
        System.out.println("Current time: " + System.currentTimeMillis());
        System.out.println("Data timeout: " + stringData.timeout());
        System.out.println("Is expired: " + (stringData.timeout() < System.currentTimeMillis()));
        
        // 存储数据
        redisCore.put(key, stringData);
        
        // 检查数据（应该被自动清理）
        boolean exists = redisCore.exist(key);
        System.out.println("Expired key exists: " + exists);
        
        RedisData retrievedData = redisCore.get(key);
        System.out.println("Retrieved expired data: " + retrievedData);
        
        // 过期数据应该被清理，所以不存在
        assertFalse("Expired key should not exist", exists);
        assertNull("Retrieved expired data should be null", retrievedData);
    }

    @Test
    public void testBytesWrapperEquality() {
        // 测试BytesWrapper的相等性
        System.out.println("\n测试BytesWrapper相等性");
        
        BytesWrapper key1 = new BytesWrapper("test_key".getBytes());
        BytesWrapper key2 = new BytesWrapper("test_key".getBytes());
        BytesWrapper key3 = new BytesWrapper("different_key".getBytes());
        
        System.out.println("Key1: " + key1);
        System.out.println("Key2: " + key2);
        System.out.println("Key3: " + key3);
        
        System.out.println("Key1 equals Key2: " + key1.equals(key2));
        System.out.println("Key1 equals Key3: " + key1.equals(key3));
        System.out.println("Key1 hashCode: " + key1.hashCode());
        System.out.println("Key2 hashCode: " + key2.hashCode());
        System.out.println("Key3 hashCode: " + key3.hashCode());
        
        // 测试存储和检索
        RedisString stringData = new RedisString();
        stringData.setValue(new BytesWrapper("test_value".getBytes()));
        
        redisCore.put(key1, stringData);
        
        boolean exists1 = redisCore.exist(key1);
        boolean exists2 = redisCore.exist(key2);
        boolean exists3 = redisCore.exist(key3);
        
        System.out.println("Exists with key1: " + exists1);
        System.out.println("Exists with key2: " + exists2);
        System.out.println("Exists with key3: " + exists3);
        
        assertTrue("Key1 should exist", exists1);
        assertTrue("Key2 should exist (same content)", exists2);
        assertFalse("Key3 should not exist", exists3);
    }
} 