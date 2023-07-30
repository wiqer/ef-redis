package com.wiqer.redis.memory;

import com.wiqer.redis.datatype.RedisBaseData;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author lilan
 */
public class RedisCache<T> {
    final static HashMap<Class<? extends RedisBaseData>, LinkedList<SimpleRingQueue<RedisBaseData>>> REDIS_CACHE = new HashMap<>();
    final static HashMap<Class<? extends RedisBaseData>, Constructor<?>> CONSTRUCTOR_CACHE = new HashMap<>();

    public  <T extends RedisBaseData> T  getRedisDataByType(Class<T> clazz) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        LinkedList<SimpleRingQueue<RedisBaseData>> dataCacheLink =  REDIS_CACHE.get(clazz);
        if(dataCacheLink == null || dataCacheLink.isEmpty()){
            return getNewRedisData(clazz);
        }
        for(SimpleRingQueue<RedisBaseData> queue : dataCacheLink){
            if(queue.isEmpty()){
                continue;
            }
            return (T) queue.poll();
        }
        return getNewRedisData(clazz);
    }

    public  <T extends RedisBaseData> void   addRedisDataToCache(T  t) {
        LinkedList<SimpleRingQueue<RedisBaseData>> dataCacheLink =  REDIS_CACHE.computeIfAbsent(t.getClass() , v -> new LinkedList<>());
        if(dataCacheLink.isEmpty()){
            dataCacheLink.add(new SimpleRingQueue<>(8888, 88888));
        }
        for(SimpleRingQueue<RedisBaseData> queue : dataCacheLink){
            if(queue.isFull()){
                continue;
            }
            queue.add(t);
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
