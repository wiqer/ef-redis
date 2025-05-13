package com.wiqer.redis.aof;


import com.wiqer.redis.MyRedisServer;
import com.wiqer.redis.RedisCore;
import com.wiqer.redis.RedisCoreImpl;
import com.wiqer.redis.command.Command;
import com.wiqer.redis.command.CommandFactory;
import com.wiqer.redis.command.WriteCommand;
import com.wiqer.redis.resp.Resp;
import com.wiqer.redis.resp.RespArray;
import com.wiqer.redis.util.Format;
import com.wiqer.redis.util.PropertiesUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * @author lilan
 */
@Slf4j
public class Aof {


    private static final String suffix = ".aof";

    /**
     * 1,经过大量测试，使用过3年以上机械磁盘的最大性能为26
     * 2,存盘偏移量，控制单个持久化文件大小
     */
    public static final int shiftBit = 26;

    private Long aofPutIndex = 0L;

    private final String fileName = PropertiesUtil.getAofPath();

    private final RingBlockingQueue<Resp> runtimeRespQueue = new RingBlockingQueue<Resp>(8888, 888888);

    ByteBuf bufferPolled = new PooledByteBufAllocator().buffer(8888, 2147483647);

    private final ScheduledThreadPoolExecutor persistenceExecutor = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "Aof_Single_Thread"));

    private final RedisCore redisCore;

    /**
     * 读写锁
     */
    final ReadWriteLock reentrantLock = new ReentrantReadWriteLock();


    public Aof(RedisCore redisCore) {
        this.redisCore = redisCore;
        File file = new File(this.fileName + suffix);
        if (!file.isDirectory()) {
            File parentFile = file.getParentFile();
            if (null != parentFile && !parentFile.exists()) {
                parentFile.mkdirs(); // 创建文件夹

            }
        }
        start();
    }

    public void put(Resp resp) {
        runtimeRespQueue.offer(resp);
    }

    public void start() {
        /**
         * 谁先执行需要顺序异步执行
         */
        persistenceExecutor.execute(this::pickupDiskDataAllSegment);
        persistenceExecutor.scheduleAtFixedRate(this::downDiskAllSegment, 10, 1, TimeUnit.SECONDS);
    }

    public void close() {
        try {
            persistenceExecutor.shutdown();
        } catch (Exception exp) {
            log.warn("Exception!", exp);
        }
    }

    /**
     * 面向过程分段存储所有的数据
     */
    public void downDiskAllSegment() {
        if (reentrantLock.writeLock().tryLock()) {
            try {
                long segmentId = -1;
                long segmentGroupHead = -1;
                /**
                 * 池化内存
                 */

                Segment:
                while (segmentId != (aofPutIndex >> shiftBit)) {
                    //要后28位
                    segmentId = (aofPutIndex >> shiftBit);
                    RandomAccessFile randomAccessFile = new RandomAccessFile(fileName + "_" + segmentId + suffix, "rw");
                    FileChannel channel = randomAccessFile.getChannel();
                    long len = channel.size();
                    int putIndex = Format.uintNBit(aofPutIndex, shiftBit);
                    long baseOffset = aofPutIndex - putIndex;

                    if (len - putIndex < 1L << (shiftBit - 2)) {
                        len = segmentId + 1 << (shiftBit - 2);
                    }
                    MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, len);

                    do {
                        //todo 序列化存储
                        Resp resp = runtimeRespQueue.peek();
                        if (resp == null) {
                            //bufferPolled.release();
                            clean(mappedByteBuffer);
                            randomAccessFile.close();
                            break Segment;
                        }
                        Resp.write(resp, bufferPolled);
                        int respLen = bufferPolled.readableBytes();
                        if ((mappedByteBuffer.capacity() <= respLen + putIndex)) {
                            len += 1L << (shiftBit - 3);
                            mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, len);
                            if (len > (1 << shiftBit)) {
                                bufferPolled.release();
                                aofPutIndex = baseOffset + 1 << shiftBit;
                                break;
                            }
                        }
                        while (respLen > 0) {
                            respLen--;
                            mappedByteBuffer.put(putIndex++, bufferPolled.readByte());
                        }
                        /**
                         * 完成消费
                         */
                        aofPutIndex = baseOffset + putIndex;
                        runtimeRespQueue.poll();
                        bufferPolled.clear();
                        if (len - putIndex < (1L << (shiftBit - 3))) {
                            len += 1L << (shiftBit - 3);
                            if (len > (1 << shiftBit)) {
                                bufferPolled.release();
                                clean(mappedByteBuffer);
                                aofPutIndex = baseOffset + 1 << shiftBit;
                                break;
                            }
                            mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, len);
                        }
                    } while (true);

                }

            } catch (IOException e) {
                System.err.println(e.getMessage());
                log.error("aof IOException ", e);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                log.error("aof Exception", e);
            } finally {
                reentrantLock.writeLock().unlock();
            }

        }
    }

    /**
     * 分段拾起所有数据
     *
     * @throws IOException
     */
    public void pickupDiskDataAllSegment() {
        if (reentrantLock.writeLock().tryLock()) {
            try {
                long segmentId = -1;
                Segment:
                while (segmentId != (aofPutIndex >> shiftBit)) {
                    //要后28位
                    segmentId = (aofPutIndex >> shiftBit);
                    RandomAccessFile randomAccessFile = new RandomAccessFile(fileName + "_" + segmentId + suffix, "r");
                    FileChannel channel = randomAccessFile.getChannel();
                    long len = channel.size();
                    int putIndex = Format.uintNBit(aofPutIndex, shiftBit);
                    long baseOffset = aofPutIndex - putIndex;

                    MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, len);
                    ByteBuf bufferPolled = new PooledByteBufAllocator().buffer((int) len);
                    bufferPolled.writeBytes(mappedByteBuffer);

                    do {
                        Resp resp = null;
                        try {
                            resp = Resp.decode(bufferPolled);
                        } catch (Exception e) {

                            clean(mappedByteBuffer);
                            randomAccessFile.close();
                            bufferPolled.release();
                            break Segment;
                        }
                        Command command = CommandFactory.from((RespArray) resp);
                        WriteCommand writeCommand = (WriteCommand) command;
                        assert writeCommand != null;
                        writeCommand.handle(this.redisCore);
                        putIndex = bufferPolled.readerIndex();
                        aofPutIndex = putIndex + baseOffset;
                        if (putIndex > (1 << shiftBit)) {
                            bufferPolled.release();
                            clean(mappedByteBuffer);
                            aofPutIndex = baseOffset + 1 << shiftBit;
                            break;
                        }

                    } while (true);

                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                reentrantLock.writeLock().unlock();
            }
        }
    }

    private static final AtomicBoolean warned = new AtomicBoolean(false);

    public static void clean(final MappedByteBuffer buffer) throws Exception {
        if (buffer == null) {
            return;
        }
        buffer.force();
        // 尝试使用现代 API 或反射清理
        if (!tryCleanWithModernApi(buffer) && !tryCleanWithReflection(buffer)) {
            if (warned.compareAndSet(false, true)) {
                log.warn(
                        "无法清理 MappedByteBuffer，可能导致内存泄漏。请考虑升级到使用 MemorySegment API。");
            }
        }
    }

    private static boolean tryCleanWithModernApi(MappedByteBuffer buffer) {
        try {
            // 尝试使用现代 API（如果可用）
            Class<?> directBufferClass = Class.forName("java.nio.DirectBuffer");
            if (directBufferClass.isInstance(buffer)) {
                Method cleanerMethod = directBufferClass.getMethod("cleaner");
                cleanerMethod.setAccessible(true);
                Object cleaner = cleanerMethod.invoke(buffer);
                if (cleaner != null) {
                    Method cleanMethod = cleaner.getClass().getMethod("clean");
                    cleanMethod.setAccessible(true);
                    cleanMethod.invoke(cleaner);
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略，尝试下一种方法
        }
        return false;
    }

    private static boolean tryCleanWithReflection(MappedByteBuffer buffer) {
        try {
            // 备选方案：使用反射和 Unsafe
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Method getUnsafeMethod = unsafeClass.getDeclaredMethod("getUnsafe");
            getUnsafeMethod.setAccessible(true);
            Object unsafe = getUnsafeMethod.invoke(null);

            Method addressMethod = buffer.getClass().getMethod("address");
            addressMethod.setAccessible(true);
            long address = (long) addressMethod.invoke(buffer);

            if (address != 0) {
                Method freeMemoryMethod = unsafeClass.getMethod("freeMemory", long.class);
                freeMemoryMethod.setAccessible(true);
                freeMemoryMethod.invoke(unsafe, address);
                return true;
            }
        } catch (Exception e) {
            // 记录详细错误
            log.debug( "清理 MappedByteBuffer 失败", e);
        }
        return false;
    }
}
