package com.wiqer.redis.memory;

import com.wiqer.redis.aof.RingBlockingQueue;
import com.wiqer.redis.datatype.RedisBaseData;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author lilan
 * addListener 和 handle 使用的是一个线程，不需要考虑线程安全问题
 * 但是aof 是另外的线程 ,当然 aof主要针对写入和删除，不存在频繁读写，所以也没必要上缓存
 */
public class RedisCache<T> {
    final static HashMap<Class<? extends RedisBaseData>, SimpleRingQueue<RedisBaseData>> REDIS_CACHE = new HashMap<>();
    final static HashMap<Class<? extends RedisBaseData>, Constructor<?>> CONSTRUCTOR_CACHE = new HashMap<>();

    public  <T extends RedisBaseData> T  getRedisDataByType(Class<T> clazz) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        SimpleRingQueue<RedisBaseData> dataCache=  REDIS_CACHE.get(clazz);
        if(dataCache == null || dataCache.isEmpty()){
            return getNewRedisData(clazz);
        }
        return (T) dataCache.poll();
    }

    public  <T extends RedisBaseData> void   addRedisDataToCache(T  t) {
        SimpleRingQueue<RedisBaseData> dataCache =  REDIS_CACHE.computeIfAbsent(t.getClass() , v -> new SimpleRingQueue<>(8888, 88888));
        if(!dataCache.isFull()){
            dataCache.add(t);
        }
    }
    /**
     * 单线程的理论上不用锁，别带入多线程思维看这段代码
     * @param clazz
     * @param <T>
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     */
    public <T extends RedisBaseData> T  getNewRedisData(Class<T> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        Constructor<T>  clsLoader = (Constructor<T>) CONSTRUCTOR_CACHE.get(clazz);
        if (clsLoader == null) {
            clsLoader = (Constructor<T>)clazz.getConstructor();
            CONSTRUCTOR_CACHE.put(clazz, clsLoader) ;
        }
        return clsLoader.newInstance();
    }
}
