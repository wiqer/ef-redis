package com.wiqer.redis.datatype;

import com.wiqer.redis.memory.RedisCache;

import java.lang.reflect.InvocationTargetException;

/**
 * @author lilan
 */
public interface RedisBaseData
{
    RedisCache<RedisBaseData> REDIS_CACHE = new RedisCache<>();

    default void clear() {

    }

    default Long getCreatedThreadId(){
        return null;
    }

    default void recovery() {
        REDIS_CACHE.addRedisDataToCache(this);
    }

    static <T extends RedisBaseData> T  getRedisDataByType(Class<T> clazz, Object... params){
        try {
            final T redisBaseData = REDIS_CACHE.getRedisDataByType(clazz);
            redisBaseData.clear();
            return  redisBaseData;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }catch (Throwable e) {
            System.out.println(e);
            throw new UnsupportedOperationException();
        }
        return null;
    }

    static <T extends RedisBaseData> T  getRedisDataByTypeAndThreadId(Class<T> clazz, Long threadId){
        try {
            final T redisBaseData = REDIS_CACHE.getRedisDataByType(clazz, threadId);
            redisBaseData.clear();
            return  redisBaseData;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }catch (Throwable e) {
            System.out.println(e);
            throw new UnsupportedOperationException();
        }
        return null;
    }
}
