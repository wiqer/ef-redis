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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;


/**
 * @author lilan
 */
public class Aof {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(Aof.class);
    private static final  String suffix=".aof";

    /**
     * 1,经过大量测试，使用过3年以上机械磁盘的最大性能为26
     * 2,存盘偏移量，控制单个持久化文件大小
     */
    public static final int shiftBit =26;
    private   Long aofPutIndex=0L;
    private  String fileName= PropertiesUtil.getAofPath();

    private RingBlockingQueue<Resp> runtimeRespQueue=new RingBlockingQueue<Resp>(8888,888888);
    ByteBuf bufferPolled= new PooledByteBufAllocator().buffer(8888, 2147483647);
//    private RingBlockingQueue<Command> initTimeCommandQueue=new RingBlockingQueue<Command>(8888,888888);
    private ScheduledThreadPoolExecutor persistenceExecutor=new ScheduledThreadPoolExecutor(2,new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Aof_Single_Thread");
        }
    });
    private final RedisCore redisCore;

    /**
     * 读写锁
     */
    final ReadWriteLock reentrantLock  =new ReentrantReadWriteLock();


    public Aof(RedisCore redisCore){
        this.redisCore = redisCore;
        File file=new File(this.fileName+suffix);
        if(!file .isDirectory())
        {
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
    public void start(){
        /**
         * 谁先执行需要顺序异步执行
         */
        persistenceExecutor.execute(()-> pickupDiskDataAllSegment());
        persistenceExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                downDiskAllSegment();
            }
        }, 10, 1, TimeUnit.SECONDS);
    }
    public void close(){
        try {
            persistenceExecutor.shutdown();
        }catch (Exception ignored) {
            LOGGER.warn( "Exception!", ignored);
        }
    }
    /**
     * 面向过程分段存储所有的数据
     */
    public void downDiskAllSegment(){
        if( reentrantLock.writeLock().tryLock()) {
            try {
                long segmentId=-1;
                long segmentGroupHead=-1;
                /**
                 * 池化内存
                 */

               Segment:
                while (segmentId!=(aofPutIndex>>shiftBit)) {
                    //要后28位
                    segmentId=(aofPutIndex>>shiftBit);
                    RandomAccessFile randomAccessFile = new RandomAccessFile(fileName+"_"+segmentId+suffix, "rw");
                    FileChannel channel= randomAccessFile.getChannel();
                    long len=channel.size();
                    int putIndex= Format.uintNBit(aofPutIndex,shiftBit) ;
                    long baseOffset=aofPutIndex-putIndex;

                    if (len-putIndex < 1L <<(shiftBit-2)){
                        len=  segmentId + 1 <<(shiftBit-2);
                    }
                    MappedByteBuffer mappedByteBuffer =channel.map(FileChannel.MapMode.READ_WRITE, 0,len);

                    do{
                        //todo 序列化存储
                        Resp resp= runtimeRespQueue.peek();
                        if(resp==null){
                            //bufferPolled.release();
                            clean(mappedByteBuffer);
                            randomAccessFile.close();
                            break  Segment;
                        }
                        Resp.write(resp,bufferPolled);
                        //ByteBuffer buffer=bufferPolled.nioBuffer();
                        //putIndex+=bufferPolled.readableBytes();
                        int respLen=bufferPolled.readableBytes();
                        if((mappedByteBuffer.capacity()<=respLen+putIndex)){
                            len+= 1L <<(shiftBit-3);
                            mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0,len);
                            if(len>(1<<shiftBit)){
                                bufferPolled.release();
                                aofPutIndex=baseOffset+1<<shiftBit;
                                break ;
                            }
                        }
                        while(respLen>0){
                            respLen--;
                            mappedByteBuffer.put(putIndex++,bufferPolled.readByte());
                        }
                        /**
                         * 完成消费
                         */
                        aofPutIndex= baseOffset+putIndex;
                        runtimeRespQueue.poll();
//                        mappedByteBuffer.put(bufferPolled.array(),0,bufferPolled.readableBytes());
//                        bufferPolled.readByte();
//                        putIndex+=bufferPolled.readableBytes();
                        bufferPolled.clear();
                        if(len-putIndex < (1L <<(shiftBit-3))){
                            len+= 1L <<(shiftBit-3);
                            if(len>(1<<shiftBit)){
                                bufferPolled.release();
                                clean(mappedByteBuffer);
                                aofPutIndex=baseOffset+1<<shiftBit;
                                break ;
                            }
                            mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0,len);
                        }



                    }while (true);

                }

            } catch (IOException e) {
                System.err.println(e.getMessage());
                LOGGER.error("aof IOException ",e);
            }catch (Exception e) {
                System.err.println(e.getMessage());
                LOGGER.error("aof Exception",e);
            }
            finally {
                reentrantLock.writeLock().unlock();
            }

        }
    }
    /**
     * 分段拾起所有数据
     * @throws IOException
     */
    public void  pickupDiskDataAllSegment()  {
        if( reentrantLock.writeLock().tryLock()) {
            try {


                long segmentId=-1;
                Segment:
                while (segmentId!=(aofPutIndex>>shiftBit)) {
                    //要后28位
                    segmentId=(aofPutIndex>>shiftBit);
                    RandomAccessFile randomAccessFile = new RandomAccessFile(fileName+"_"+segmentId+suffix, "r");
                    FileChannel channel= randomAccessFile.getChannel();
                    long len=channel.size();
                    int putIndex= Format.uintNBit(aofPutIndex,shiftBit) ;
                    long baseOffset=aofPutIndex-putIndex;

                    MappedByteBuffer mappedByteBuffer =channel.map(FileChannel.MapMode.READ_ONLY, 0,len);
                    ByteBuf bufferPolled= new PooledByteBufAllocator().buffer((int) len);
                    bufferPolled.writeBytes(mappedByteBuffer);

                    do{
                        Resp  resp=null;
                        try {
                            resp=Resp.decode(bufferPolled);
                        }catch (Exception e) {

                            clean(mappedByteBuffer);
                            randomAccessFile.close();
                            bufferPolled.release();
                            break Segment;
                        }
                        Command command = CommandFactory.from((RespArray) resp);
                        WriteCommand writeCommand=   (WriteCommand)command;
                        writeCommand.handle(this.redisCore);
                        putIndex=bufferPolled.readerIndex();
                        aofPutIndex=putIndex+ baseOffset;
                        if(putIndex>(1<<shiftBit)){
                            bufferPolled.release();
                            clean(mappedByteBuffer);
                            aofPutIndex=baseOffset+1<<shiftBit;
                            break ;
                        }

                    }while (true);

                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                reentrantLock.writeLock().unlock();
            }
        }
    }


    /*
     * 其实讲到这里该问题的解决办法已然清晰明了了——就是在删除索引文件的同时还取消对应的内存映射，删除mapped对象。
     * 不过令人遗憾的是，Java并没有特别好的解决方案——令人有些惊讶的是，Java没有为MappedByteBuffer提供unmap的方法，
     * 该方法甚至要等到Java 10才会被引入 ,DirectByteBufferR类是不是一个公有类
     * class DirectByteBufferR extends DirectByteBuffer implements DirectBuffer 使用默认访问修饰符
     * 不过Java倒是提供了内部的“临时”解决方案——DirectByteBufferR.cleaner().clean() 切记这只是临时方法，
     * 毕竟该类在Java9中就正式被隐藏了，而且也不是所有JVM厂商都有这个类。
     * 还有一个解决办法就是显式调用System.gc()，让gc赶在cache失效前就进行回收。
     * 不过坦率地说，这个方法弊端更多：首先显式调用GC是强烈不被推荐使用的，
     * 其次很多生产环境甚至禁用了显式GC调用，所以这个办法最终没有被当做这个bug的解决方案。
     */
    public static void clean(final MappedByteBuffer buffer) throws Exception {
        if (buffer == null) {
            return;
        }
        buffer.force();
        AccessController.doPrivileged(new PrivilegedAction<Object>() {//Privileged特权
            @Override
            public Object run() {
                try {
                    // System.out.println(buffer.getClass().getName());
                    Method getCleanerMethod = buffer.getClass().getMethod("cleaner", new Class[0]);
                    getCleanerMethod.setAccessible(true);
                    sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(buffer, new Object[0]);
                    cleaner.clean();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }
}
