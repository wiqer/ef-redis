package com.wiqer.redis.datatype;

import com.wiqer.redis.memory.RedisCache;

import java.lang.reflect.InvocationTargetException;

/**
 * @author lilan
 */
public interface RedisBaseData
{
    RedisCache<RedisBaseData> REDIS_CACHE = new RedisCache<>();

    void clear();

    default void recovery() {
        try{
            clear();
            REDIS_CACHE.addRedisDataToCache(this);
        }catch (Exception e){
            System.out.println(e);
            throw new UnsupportedOperationException();
        }
    }

    static <T extends RedisBaseData> T  getRedisDataByType(Class<T> clazz, Object... params){
        try {
            return REDIS_CACHE.getRedisDataByType(clazz);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }catch (Exception e) {
            System.out.println(e);
            throw new UnsupportedOperationException();
        }
        return null;
    }
}
