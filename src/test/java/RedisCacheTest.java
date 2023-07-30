import com.wiqer.redis.datatype.RedisBaseData;
import com.wiqer.redis.memory.RedisCache;
import com.wiqer.redis.memory.SimpleRingQueue;
import com.wiqer.redis.resp.*;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;

public class RedisCacheTest {
    @Test
    public void main() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        RedisCache<RedisBaseData> redisCache = new RedisCache<>();
        redisCache.getRedisDataByType(RespInt.class);
        Resp[] array =  new Resp[]{new RespInt(1),new RespInt(1)};
        redisCache.getRedisDataByType(RespArray.class);
        SimpleString ss = redisCache.getRedisDataByType(SimpleString.class);
        System.out.println(ss);
    }
}
