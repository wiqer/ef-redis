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

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
                ByteBuf bufferPolled= new PooledByteBufAllocator().buffer(8888);
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
                        Resp resp= runtimeRespQueue.poll();
                        if(resp==null){
                            bufferPolled.release();
                            break  Segment;
                        }
                        Resp.write(resp,bufferPolled);
                        //ByteBuffer buffer=bufferPolled.nioBuffer();
                        //putIndex+=bufferPolled.readableBytes();
                        int respLen=bufferPolled.readableBytes();
                        while(respLen>0){
                            respLen--;
                            mappedByteBuffer.put(putIndex++,bufferPolled.readByte());
                        }
                        aofPutIndex= baseOffset+putIndex;

//                        mappedByteBuffer.put(bufferPolled.array(),0,bufferPolled.readableBytes());
//                        bufferPolled.readByte();
//                        putIndex+=bufferPolled.readableBytes();
                        bufferPolled.clear();
                        if(putIndex>(1<<shiftBit)){
                            bufferPolled.release();
                            break ;

                        }
                        if(len-putIndex < 1L <<(shiftBit-3)){
                            len+= 1L <<(shiftBit-3);
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
                            break ;
                        }

                    }while (true);

                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                reentrantLock.writeLock().unlock();
            }
        }
    }
}
