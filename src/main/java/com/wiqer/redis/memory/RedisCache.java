package com.wiqer.redis.memory;

import com.wiqer.redis.aof.RingBlockingQueue;
import com.wiqer.redis.datatype.RedisBaseData;
import sun.applet.Main;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lilan
 * addListener 和 handle 使用的是一个线程，不需要考虑线程安全问题
 * 但是aof 是另外的线程 ,当然 aof主要针对写入和删除，不存在频繁读写，所以也没必要上缓存
 */
public class RedisCache<T> {
    final static HashMap<Class<? extends RedisBaseData>, SimpleRingQueue<RedisBaseData>> REDIS_CACHE = new HashMap<>();
    final static HashMap<Class<? extends RedisBaseData>, Constructor<?>> CONSTRUCTOR_CACHE = new HashMap<>();

    final static HashMap<Class<? extends RedisBaseData>, SimpleRingQueue<RedisBaseData>[]> CONSTRUCTOR_MULTI_THREAD_WRITE_CACHE = new HashMap<>();
    final static HashMap<Long, HashMap<Class<? extends RedisBaseData>, RingBlockingQueue<RedisBaseData>>> CONSTRUCTOR_MULTI_THREAD_READ_CACHE = new HashMap<>();
    final static int  MULTI_THREAD_CACHE_ARRAY_SIZE = 4;
    volatile static int  readIndex = 0;
    volatile static int  writeIndex = 2;
    private static final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1,new ThreadFactory() {
        private AtomicInteger index = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "scavenge_" + index.getAndIncrement());
        }
    });
    private volatile static boolean running = false;
    public static void start() {
        if (running) {
            return;
        }

        synchronized (CONSTRUCTOR_MULTI_THREAD_WRITE_CACHE) {
            if (running) {
                return;
            }
            running = true;
            scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    scavenge();
                }

            }, 10, 10, TimeUnit.MILLISECONDS);
        }
    }
    static {
       // start();
    }

    private static void scavenge() {
        int copyIndex = (writeIndex - 1) & 3;
        for(Map.Entry<Class<? extends RedisBaseData>, SimpleRingQueue<RedisBaseData>[]> entry : CONSTRUCTOR_MULTI_THREAD_WRITE_CACHE.entrySet()){
            final  SimpleRingQueue<RedisBaseData>[] simpleRingQueues = entry.getValue();
            if(simpleRingQueues == null){
                continue;
            }
            SimpleRingQueue<RedisBaseData> simpleRingQueue = simpleRingQueues[copyIndex];
            if(simpleRingQueue == null || simpleRingQueue.isEmpty()){
                continue;
            }
            while (true){
                RedisBaseData redisBaseData = simpleRingQueue.poll();
                if (redisBaseData == null){
                    break;
                }
                final Long threadId = redisBaseData.getCreatedThreadId();
                HashMap<Class<? extends RedisBaseData>, RingBlockingQueue<RedisBaseData>> cacheMap = CONSTRUCTOR_MULTI_THREAD_READ_CACHE.computeIfAbsent(threadId , v -> new HashMap<>());
                final  RingBlockingQueue ringBlockingQueue= cacheMap.computeIfAbsent(entry.getKey() , v -> new RingBlockingQueue<>(8888, 88888));
                ringBlockingQueue.add(redisBaseData);
            }
        }
        readIndex = readIndex++ & 3;
        writeIndex = writeIndex++ & 3;
    }

    public  <T extends RedisBaseData> T  getRedisDataByType(Class<T> clazz) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        SimpleRingQueue<RedisBaseData> dataCache=  REDIS_CACHE.get(clazz);
        if(dataCache == null || dataCache.isEmpty()){
            return getNewRedisData(clazz);
        }
        return (T) dataCache.poll();
    }
    public  <T extends RedisBaseData> T  getRedisDataByType(Class<T> clazz, Long threadId) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        HashMap<Class<? extends RedisBaseData>, RingBlockingQueue<RedisBaseData>> multiRedisCache = CONSTRUCTOR_MULTI_THREAD_READ_CACHE.get(threadId);
        if(multiRedisCache == null){
            return getNewRedisData(clazz);
        }
        RingBlockingQueue<RedisBaseData> dataCache =  multiRedisCache.get(clazz);
        if(dataCache == null || dataCache.isEmpty()){
            return getNewRedisData(clazz);
        }
        T redisBaseData = (T) dataCache.poll();
        if(redisBaseData == null){
            return getNewRedisData(clazz);
        }
        return redisBaseData;
    }
    public  <T extends RedisBaseData> void addRedisDataToCache(T  t) {
        if(t.getCreatedThreadId() == null){
            SimpleRingQueue<RedisBaseData> dataCache =  REDIS_CACHE.computeIfAbsent(t.getClass() , v -> new SimpleRingQueue<>(8888, 88888));
            if(!dataCache.isFull()){
                dataCache.add(t);
            }
        }else {
            SimpleRingQueue<RedisBaseData>[] dataCacheArr =   CONSTRUCTOR_MULTI_THREAD_WRITE_CACHE.computeIfAbsent(t.getClass() , v -> new SimpleRingQueue[MULTI_THREAD_CACHE_ARRAY_SIZE]);
           if(dataCacheArr[writeIndex] == null){
               dataCacheArr[writeIndex] = new SimpleRingQueue<RedisBaseData>(8888, 88888);
           }
            SimpleRingQueue<RedisBaseData> dataCache =  dataCacheArr[writeIndex];
            if(!dataCache.isFull()){
                dataCache.add(t);
            }
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
            synchronized (this){
                clsLoader = (Constructor<T>)clazz.getConstructor();
                CONSTRUCTOR_CACHE.put(clazz, clsLoader) ;
            }
        }
        return clsLoader.newInstance();
    }
}
