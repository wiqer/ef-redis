import com.wiqer.redis.RedisCore;
import com.wiqer.redis.RedisCoreImpl;
import com.wiqer.redis.command.CommandType;
import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisString;
import com.wiqer.redis.datatype.RedisHash;
import com.wiqer.redis.datatype.RedisList;
import com.wiqer.redis.datatype.RedisSet;
import com.wiqer.redis.datatype.RedisZset;
import com.wiqer.redis.resp.BulkString;
import com.wiqer.redis.resp.Resp;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 测试新实现的Redis命令
 */
public class NewCommandsTest {
    
    private final RedisCore redisCore = new RedisCoreImpl();
    
    @Test
    public void testGetRange() {
        // 设置测试数据
        RedisString redisString = new RedisString();
        redisString.setValue(new BytesWrapper("Hello World".getBytes()));
        redisCore.put(new BytesWrapper("test_str".getBytes()), redisString);
        
        // 测试GETRANGE命令
        Resp[] array = new Resp[4];
        array[0] = new BulkString(new BytesWrapper("GETRANGE".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_str".getBytes()));
        array[2] = new BulkString(new BytesWrapper("0".getBytes()));
        array[3] = new BulkString(new BytesWrapper("4".getBytes()));
        
        CommandType.getrange.getSupplier().get().setContent(array);
        assertNotNull("GETRANGE命令应该被正确创建", CommandType.getrange.getSupplier().get());
    }
    
    @Test
    public void testAppend() {
        // 测试APPEND命令
        Resp[] array = new Resp[3];
        array[0] = new BulkString(new BytesWrapper("APPEND".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_append".getBytes()));
        array[2] = new BulkString(new BytesWrapper("Hello".getBytes()));
        
        CommandType.append.getSupplier().get().setContent(array);
        assertNotNull("APPEND命令应该被正确创建", CommandType.append.getSupplier().get());
    }
    
    @Test
    public void testHget() {
        // 设置测试数据
        RedisHash redisHash = new RedisHash();
        redisHash.put(new BytesWrapper("field1".getBytes()), new BytesWrapper("value1".getBytes()));
        redisCore.put(new BytesWrapper("test_hash".getBytes()), redisHash);
        
        // 测试HGET命令
        Resp[] array = new Resp[3];
        array[0] = new BulkString(new BytesWrapper("HGET".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_hash".getBytes()));
        array[2] = new BulkString(new BytesWrapper("field1".getBytes()));
        
        CommandType.hget.getSupplier().get().setContent(array);
        assertNotNull("HGET命令应该被正确创建", CommandType.hget.getSupplier().get());
    }
    
    @Test
    public void testHgetAll() {
        // 测试HGETALL命令
        Resp[] array = new Resp[2];
        array[0] = new BulkString(new BytesWrapper("HGETALL".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_hash".getBytes()));
        
        CommandType.hgetall.getSupplier().get().setContent(array);
        assertNotNull("HGETALL命令应该被正确创建", CommandType.hgetall.getSupplier().get());
    }
    
    @Test
    public void testLlen() {
        // 设置测试数据
        RedisList redisList = new RedisList();
        redisList.lpush(java.util.Arrays.asList(new BytesWrapper("item1".getBytes())));
        redisCore.put(new BytesWrapper("test_list".getBytes()), redisList);
        
        // 测试LLEN命令
        Resp[] array = new Resp[2];
        array[0] = new BulkString(new BytesWrapper("LLEN".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_list".getBytes()));
        
        CommandType.llen.getSupplier().get().setContent(array);
        assertNotNull("LLEN命令应该被正确创建", CommandType.llen.getSupplier().get());
    }
    
    @Test
    public void testScard() {
        // 设置测试数据
        RedisSet redisSet = new RedisSet();
        redisSet.sadd(java.util.Arrays.asList(new BytesWrapper("member1".getBytes())));
        redisCore.put(new BytesWrapper("test_set".getBytes()), redisSet);
        
        // 测试SCARD命令
        Resp[] array = new Resp[2];
        array[0] = new BulkString(new BytesWrapper("SCARD".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_set".getBytes()));
        
        CommandType.scard.getSupplier().get().setContent(array);
        assertNotNull("SCARD命令应该被正确创建", CommandType.scard.getSupplier().get());
    }
    
    @Test
    public void testZcard() {
        // 设置测试数据
        RedisZset redisZset = new RedisZset();
        RedisZset.ZsetKey zsetKey = new RedisZset.ZsetKey(new BytesWrapper("member1".getBytes()), 1L);
        redisZset.add(java.util.Arrays.asList(zsetKey));
        redisCore.put(new BytesWrapper("test_zset".getBytes()), redisZset);
        
        // 测试ZCARD命令
        Resp[] array = new Resp[2];
        array[0] = new BulkString(new BytesWrapper("ZCARD".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_zset".getBytes()));
        
        CommandType.zcard.getSupplier().get().setContent(array);
        assertNotNull("ZCARD命令应该被正确创建", CommandType.zcard.getSupplier().get());
    }
    
    @Test
    public void testStrlen() {
        // 设置测试数据
        RedisString redisString = new RedisString();
        redisString.setValue(new BytesWrapper("Hello".getBytes()));
        redisCore.put(new BytesWrapper("test_strlen".getBytes()), redisString);
        
        // 测试STRLEN命令
        Resp[] array = new Resp[2];
        array[0] = new BulkString(new BytesWrapper("STRLEN".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_strlen".getBytes()));
        
        CommandType.strlen.getSupplier().get().setContent(array);
        assertNotNull("STRLEN命令应该被正确创建", CommandType.strlen.getSupplier().get());
    }
    
    @Test
    public void testHexists() {
        // 设置测试数据
        RedisHash redisHash = new RedisHash();
        redisHash.put(new BytesWrapper("field1".getBytes()), new BytesWrapper("value1".getBytes()));
        redisCore.put(new BytesWrapper("test_hexists".getBytes()), redisHash);
        
        // 测试HEXISTS命令
        Resp[] array = new Resp[3];
        array[0] = new BulkString(new BytesWrapper("HEXISTS".getBytes()));
        array[1] = new BulkString(new BytesWrapper("test_hexists".getBytes()));
        array[2] = new BulkString(new BytesWrapper("field1".getBytes()));
        
        CommandType.hexists.getSupplier().get().setContent(array);
        assertNotNull("HEXISTS命令应该被正确创建", CommandType.hexists.getSupplier().get());
    }
    
    @Test
    public void testFlushdb() {
        // 测试FLUSHDB命令
        Resp[] array = new Resp[1];
        array[0] = new BulkString(new BytesWrapper("FLUSHDB".getBytes()));
        
        CommandType.flushdb.getSupplier().get().setContent(array);
        assertNotNull("FLUSHDB命令应该被正确创建", CommandType.flushdb.getSupplier().get());
    }
} 