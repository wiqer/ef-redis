package com.wiqer.redis;

import com.wiqer.redis.datatype.BytesWrapper;
import com.wiqer.redis.datatype.RedisData;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author lilan
 */
public class RedisCoreImpl implements RedisCore {

    int selectKeySum = 100;
    /**
     * 客户端可能使用hash路由，更换为跳表更好的避免hash冲突
     * 这里用hash（o1）和跳表（ologn）性能区别不大，key不会太多，瓶颈主要在io和cpu
     */
    private final Map<BytesWrapper, RedisData> map = new ConcurrentSkipListMap<>();


    private final ConcurrentHashMap<BytesWrapper, Channel> clients = new ConcurrentHashMap<>();
    private final Map<Channel, BytesWrapper> clientNames = new ConcurrentHashMap<>();

    @Override
    public Set<BytesWrapper> keys() {
        return map.keySet();
    }

    @Override
    public void putClient(BytesWrapper connectionName, Channel channelContext) {
        clients.put(connectionName, channelContext);
        clientNames.put(channelContext, connectionName);
    }

    @Override
    public boolean exist(BytesWrapper key) {
        RedisData data = get(key);
        return data != null;
    }

    @Override
    public void put(BytesWrapper key, RedisData redisData) {
        map.put(key, redisData);
    }

    @Override
    public RedisData get(BytesWrapper key) {
        RedisData redisData = map.get(key);
        if (redisData == null) {
            return null;
        }
        if (redisData.timeout() == -1) {
            return redisData;
        }
        if (redisData.timeout() < System.currentTimeMillis()) {
            map.remove(key);
            return null;
        }
        return redisData;
    }

    @Override
    public long remove(List<BytesWrapper> keys) {
        return keys.stream().peek(map::remove).count();
    }

    @Override
    public void cleanAll() {
        map.clear();
    }

    @Override
    public void startTllTask(int hz,long maxMemory) {
        // 计算每次执行的间隔时间（毫秒）
        long interval = 1000 / hz;

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);


        // 执行定时任务
        executorService.scheduleAtFixedRate(() -> {
            long startTime = System.currentTimeMillis();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            if(isJvmMemoryExceeded(maxMemory)){

                while (interval + startTime > System.currentTimeMillis()){
                    // 这里放置具体要执行的任务
                    final int size = map.size();
                    selectKeySum = Math.min(selectKeySum, size);
                    for (Map.Entry<BytesWrapper, RedisData> entry : map.entrySet()){
                        if(selectKeySum == size || selectKeySum > random.nextInt(size)){
                            long timeout = entry.getValue().timeout();
                           if(timeout == -1){
                               continue;
                           }
                           if(timeout < System.currentTimeMillis()){
                               map.remove(entry.getKey());
                           }
                        }
                    }
                    long endTime = System.currentTimeMillis();
                    if(interval + startTime > endTime){
                        selectKeySum = (int) (selectKeySum * (float)interval/ (float)(interval + startTime - endTime));
                    }else {
                        selectKeySum = (int) (selectKeySum * 0.75);
                    }
                    selectKeySum = Math.max(selectKeySum, 1);
                }


            }

        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    public static boolean isJvmMemoryExceeded(long maxMemory) {
        if (maxMemory < 0) {
            return false;
        }
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory > maxMemory;
    }
}
